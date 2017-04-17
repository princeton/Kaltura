package edu.princeton;
/**
 *  This application is used to report the storage used by videos
 *  in a particular category.
 *
 * @author Mark Ratliff, Princeton University
 */

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.List;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import com.kaltura.client.KalturaApiException;
import com.kaltura.client.KalturaClient;
import com.kaltura.client.KalturaConfiguration;
import com.kaltura.client.enums.KalturaEntryStatus;
import com.kaltura.client.enums.KalturaSessionType;
import com.kaltura.client.services.KalturaMediaService;
import com.kaltura.client.types.KalturaMediaEntry;
import com.kaltura.client.types.KalturaMediaListResponse;
import com.kaltura.client.types.KalturaFilterPager;
import com.kaltura.client.types.KalturaMediaEntryFilter;
import com.kaltura.client.types.KalturaFlavorAsset;
import com.kaltura.client.services.KalturaFlavorAssetService;

import org.apache.log4j.Logger;

public class ReportStorageByCategory {

	// Get the log file writer
	private static Logger logger = Logger.getLogger(ReportStorageByCategory.class);
	
	// Define configuration variables
	private static final  int PARTNER_ID;
	private static final  String SECRET;
	private static final  String ADMIN_SECRET;
	private static final String ENDPOINT;
	private static final String USER_ID;
	private static final String STORAGE_REPORT_FILE;
	private static final int target_category;
	
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
		
		STORAGE_REPORT_FILE = rb.getString("storage_report_file")
		
		target_category = Integer.parseInt(rb.getString("target_category"));
	}
	
	// The Kaltura client that will be used to perform the administrative tasks
	static private KalturaClient client;

	/**
	 * The main method simply calls the reportStorage() method
	 */
	public static void main(String[] args) {

		logger.info("=====================================================================================");
		logger.info("Running ReportStorageByCategory application on category "+target_category);
		try 
		{
			reportStorage(target_category);
		} 
		catch (KalturaApiException e) 
		{
			logger.error("Application failed", e);
		}
		catch (IOException e) 
		{
			logger.error("Application failed", e);
		}
		
		logger.info("Application ReportStorageByCategory completed.");
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
	 * Report storage used by all videos in a category
	 * 
	 * @param category_id The ID of the category containing the videos
	 */
	private static void reportStorage(int category_id) throws KalturaApiException, IOException {

		KalturaClient kclient = getKalturaClient();

		// Create the filter that will return videos only from the specified category
		KalturaMediaEntryFilter filter = null;
		
		if (category_id >= 0)
		{
			filter = new KalturaMediaEntryFilter();
			
			// Return only videos in this category
			filter.categoryAncestorIdIn = String.valueOf(category_id);
			
			// Return only videos with status = READY
			//filter.statusEqual = KalturaEntryStatus.READY;
		}

		// Create a pager that will allow us to work with the list in batches ("pages") of 500
		KalturaFilterPager pager = new KalturaFilterPager();
		pager.pageSize = 500;
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

		// Step through each batch/page, write output to file
		File outputfile = new File(STORAGE_REPORT_FILE);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputfile));
		
    	// Set date format for input
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT-5"));
		
		while (pager.pageIndex <= num_pages)
		{
			list = kclient.getMediaService().list(filter, pager);
			//int[] transcodeProfIds = new int[] {0, 487041, 487071, 487081, 487091, 616982, 616992, 617002, 702661};
			int[] transcodeProfIds = new int[] {487041};
			
			// Step through each media entry in the batch/page
			for (KalturaMediaEntry entry : list.objects) 
			{
				int orig_size = 0;
				int total_flav_size = 0;
				int total_size = 0;
				String lastPlayedDate = null;
				String createdDate = null;
				
				String delete_flav_id = null;
				
				// Get the list of flavors associated with this entry
				List<KalturaFlavorAsset> kassetflavlist = kclient.getFlavorAssetService().getByEntryId(entry.id);
                Iterator iter = kassetflavlist.iterator();
                
                while (iter.hasNext())
                {
                	// Get the path to the flavor
                	// KalturaRemotePathListResponse krplr = kclient.getFlavorAssetService().getRemotePaths(entry_id);
                	
                	KalturaFlavorAsset kassetflav = (KalturaFlavorAsset) iter.next();
                   	if (kassetflav.isOriginal)
                   	{
                   		orig_size = kassetflav.size;
                   	}
                   	
                   	// Get flavors with height >= some value
                   	//else if (kassetflav.height >= 1080)
                   	
                   	// Get flavors resulting from specific transcoding profiles
                   	//if (intArrayContains(transcodeProfIds, kassetflav.flavorParamsId)) 
                   	else
                   	{
                   		//delete_flav_id = kassetflav.id;
                   		//System.out.println("Flavor: "+kassetflav.flavorParamsId+"  Was not in array: "+transcodeProfIds);
                   		total_flav_size += kassetflav.size;
                   		
                   		// Delete this flavor?
                   		//logger.info("Deleting flavor asset: "+kassetflav.id);
                   		
                   		//kclient.getFlavorAssetService().delete(kassetflav.id);
                   		//System.exit(0);
                   		
                   		// Test recreation of flavor
                   		//kclient.getFlavorAssetService().convert("0_4b1r3wfb", 487041);
                   		//System.exit(0);
                   	}
                }
                
                total_size = orig_size + total_flav_size;
                	
            	// Get Date Created
            	Date date = new Date(((long) entry.createdAt) * 1000L);
            	createdDate = sdf.format(date);
            	
            	// Get Date Last Played
                if (entry.lastPlayedAt <= 0)
                {
                	lastPlayedDate = "NA";
                }
                else
                {
                	date = new Date(((long) entry.lastPlayedAt) * 1000L);
                	lastPlayedDate = sdf.format(date);
                }
                
                //if (delete_flav_id != null) {
                output.write(
                		entry.id+
                		//",\"" + entry.name + "\""+
                		","+createdDate+
                        ","+entry.plays+
                        //", "+entry.lastPlayedAt+
                        ","+lastPlayedDate+
                        ",\""+entry.creatorId+"\""+
                        ","+orig_size+
                        //","+delete_flav_id+
                        ","+total_flav_size+
                        //","+total_size+
                        "\n"
                );
                //}
			}
			
			++pager.pageIndex;
			logger.info("pager.pageIndex = " + pager.pageIndex);
		}
		
		output.close();
	}
	
	private static boolean intArrayContains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }
}
