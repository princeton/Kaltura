/**
 *  This application is used to apply the a specific access profile to
 *   all videos under a given category.
 *
 * @author Mark Ratliff, Princeton University
 */

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

//import javax.swing.text.html.HTMLDocument.Iterator;

import com.kaltura.client.KalturaApiException;
import com.kaltura.client.KalturaClient;
import com.kaltura.client.KalturaConfiguration;
import com.kaltura.client.KalturaMultiResponse;
import com.kaltura.client.enums.KalturaEntryStatus;
import com.kaltura.client.enums.KalturaMediaType;
import com.kaltura.client.enums.KalturaSessionType;
import com.kaltura.client.services.KalturaMediaService;
import com.kaltura.client.types.KalturaMediaEntry;
import com.kaltura.client.types.KalturaMediaListResponse;
import com.kaltura.client.types.KalturaPartner;
import com.kaltura.client.types.KalturaUploadToken;
import com.kaltura.client.types.KalturaUploadedFileTokenResource;
import com.kaltura.client.types.KalturaFilterPager;
import com.kaltura.client.types.KalturaMediaEntryFilter;

import com.kaltura.client.types.KalturaFlavorAsset;
import com.kaltura.client.services.KalturaFlavorAssetService;

import com.kaltura.client.test.KalturaTestConfig;
import com.kaltura.client.test.TestUtils;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class SetAccessByCategory {

	// Get the log file writer
	private static Logger logger = Logger.getLogger(SetAccessByCategory.class);
	
	// Define configuration variables
	private static final  int PARTNER_ID;
	private static final  String SECRET;
	private static final  String ADMIN_SECRET;
	private static final String ENDPOINT;
	private static final String USER_ID;
	private static final int target_category;
	private static final int target_access_profile_id;
	
	// Load configuration values from the config file
	static 
	{	
        // The name of the configuration file from which values will be loaded
		String config_file_name = "config";

		ResourceBundle rb = ResourceBundle.getBundle(config_file_name);

		ENDPOINT = rb.getString("ENDPOINT");
		PARTNER_ID = Integer.parseInt(rb.getString("PARTNER_ID"));
		SECRET = rb.getString("SECRET");
		ADMIN_SECRET = rb.getString("ADMIN_SECRET");	
		USER_ID = rb.getString("USER_ID");
		
		target_category = Integer.parseInt(rb.getString("target_category"));
		target_access_profile_id = Integer.parseInt(rb.getString("target_access_profile_id"));
	}
	
	// The Kaltura client that will be used to perform the administrative tasks
	static private KalturaClient client;

	/**
	 * The main method simply calls the setAccess() method
	 */
	public static void main(String[] args) {

		logger.info("=====================================================================================");
		logger.info("Running SetAccessByCategory application on category "+target_category+
				  " applying access profile "+target_access_profile_id);
		try 
		{
			setAccess(target_category, target_access_profile_id);
		} 
		catch (KalturaApiException e) 
		{
			logger.error("Application failed", e);
		}
		
		logger.info("Application SetAccessByCategory completed.");
		logger.info("=====================================================================================");
	}

	/**
	 * Helper function to create the Kaltura client object once and then reuse a static instance.
	 * @return a singleton of <code>KalturaClient</code> used in this case 
	 * @throws KalturaApiException if failed to generate session
	 */
	private static KalturaClient getKalturaClient() throws KalturaApiException
	{
		if (client != null) {
			return client;
		}

		// Generate KalturaConfiguration object
		KalturaConfiguration config = new KalturaConfiguration();
		config.setPartnerId(PARTNER_ID);
		config.setEndpoint(ENDPOINT);

		try 
		{
			// Create the client and open a session
			client = new KalturaClient(config);
			String ks = client.generateSession(ADMIN_SECRET, USER_ID, KalturaSessionType.ADMIN, PARTNER_ID);
			client.setSessionId(ks);
		} 
		catch(Exception ex) 
		{
			client = null;
			throw new KalturaApiException("Failed to generate session");
		}

		logger.info("Generated Kaltura session ID: " + client.getSessionId());
		
		return client;
	}

	/** 
	 * Set the access profile on all videos in a category
	 * 
	 * @param category_id The ID of the category containing the videos
	 * @param access_control_id The ID of the access control profile that will be applied to the videos
	 */
	private static void setAccess(int category_id, int access_control_id) throws KalturaApiException {

		KalturaClient kclient = getKalturaClient();

		// Create the filter that will return videos only from the specified category
		KalturaMediaEntryFilter filter = new KalturaMediaEntryFilter();
		filter.categoryAncestorIdIn = String.valueOf(category_id);
		filter.statusEqual = KalturaEntryStatus.READY;

		// Create a pager that will allow us to work with the list in batches ("pages") of 50
		KalturaFilterPager pager = new KalturaFilterPager();
		pager.pageSize = 50;
		pager.pageIndex = 1;

		// Get the list of videos in the category
		KalturaMediaListResponse list;
		list = kclient.getMediaService().list(filter, pager);

		// Get the total number of videos
		int total_num = list.totalCount;
		logger.info("Total number of entries found in the category: "+total_num);

		// Compute the number of batches/pages
		int num_pages = total_num/pager.pageSize;
		if (total_num%pager.pageSize > 0) ++num_pages;

		// Step through each batch/page
		while (pager.pageIndex <= num_pages)
		{
			list = kclient.getMediaService().list(filter, pager);
			
			// Step through each media entry in the batch/page
			for (KalturaMediaEntry entry : list.objects) 
			{

				// If the entry's access control profile is not what we intend,
				//  then set it.
				if (entry.accessControlId != access_control_id)
				{
					logger.info("Changing access profile for ENTRY_ID: "+entry.id);
					logger.info("Entry Title: '"+entry.name+"'");
					logger.info("Original Entry Access Control Profile: "+entry.accessControlId);
					logger.info("New Entry Access Control Profile: "+access_control_id);
					
					// Create an empty KalturaMediaEntry object with only the accessControlId defined
					KalturaMediaEntry entryUpdate = new KalturaMediaEntry();
					entryUpdate.accessControlId = access_control_id;  

					// Update the Entry
					kclient.getMediaService().update(entry.id, entryUpdate);

					// Exit after processing one entry_id when debugging
					//System.exit(0);
				}
			}
			
			++pager.pageIndex;
		}
	}
}
