import urllib
import os
from KalturaClient import *
from KalturaClient.Plugins.Core import *
from datetime import datetime

'''
Runs only with Python v2.x

Library/SDK dependencies:

To install Kaltura Python SDK

curl -O http://cdnbakmi.kaltura.com/content/clientlibs/python_02-03-2017.tar.gz

cd python

sudo python27 setup.py install

To install poser SDK

curl -O https://pypi.python.org/packages/9f/dc/0683a458d21c3d561ab2f71b4fcdd812bf04e55c54e560b0854cea95610e/poster-0.8.1.tar.gz#md5=2db12704538781fbaa7e63f1505d6fc8

cd poster

sudo python27 setup.py install

To install boto3 SDK for AWS

sudo pip install boto3

'''

partnerId = os.getenv("KALTURA_PARTNERID")
secret = os.getenv("KALTURA_SECRET")
userId = os.getenv("KALTURA_USERID")
config = KalturaConfiguration(partnerId)
config.serviceUrl = "https://www.kaltura.com/"
client = KalturaClient(config)
ktype = KalturaSessionType.ADMIN
expiry = 432000 # 432000 = 5 days
privileges = "disableentitlement"


def startsession():
	""" Use configuration to generate KS
	"""
	ks = client.session.start(secret, userId, ktype, partnerId, expiry, privileges)
	client.setKs(ks)



def listEntries():
	""" List entries - Returns strings - Requires a KS generated for the client
	"""
	startsession()

	# Get list
	filter = KalturaMediaEntryFilter()
	#filter.orderBy = "-createdAt" # Newest first
	filter.orderBy = "+createdAt" # Oldest first
	pager = KalturaFilterPager()
	pager.pageSize = 500
	pager.pageIndex = 1

	entrylist = client.media.list(filter, pager)
	totalcount = entrylist.totalCount

	# Loop
	nid = 1
	#while nid < totalcount :
	while nid < 3:
          entrylist = client.media.list(filter, pager)

          # Print entry_id, date created, date last played
          for entry in entrylist.objects:

            if entry.createdAt > 0:
              createdAt_str = datetime.fromtimestamp(entry.createdAt).strftime('%Y-%m-%d %H:%M:%S')
            else:
              createdAt_str = "NULL"

            if entry.lastPlayedAt > 0:
              lastPlayedAt_str = datetime.fromtimestamp(entry.createdAt).strftime('%Y-%m-%d %H:%M:%S')
            else:
              lastPlayedAt_str = "NULL"

#            print ("id = %s, createdAt = %s, lastPlayedAt = %s" % (entry.id, createdAt_str, lastPlayedAt_str))
            nid = nid + 1

          pager.pageIndex = pager.pageIndex + 1

          entry_id = "1_i1z6di04"
          src_id = ""
          flavor_id = ""
          flavorassetswparamslist = client.flavorAsset.getFlavorAssetsWithParams(entry_id)
          print(type(flavorassetswparamslist).__name__)

# TODO:
# Delete flavors EXCEPT for source 
# client.flavorAsset.delete(flavorasset_id)

          for flavorassetwparams in flavorassetswparamslist:
            print(type(flavorassetwparams).__name__)
            flavorasset = flavorassetwparams.getFlavorAsset()
            if flavorasset.getIsOriginal():
              print(type(flavorasset).__name__)
              print(flavorasset.id)            
              src_id = flavorasset.id
              break

          #print("\n".join(map(str, flavorassets)))


          # Get the Download URL of the source video
          src_url = client.flavorAsset.getUrl(src_id)
          print("URL of src = %s" % src_url)

          # Download the source video
#          urllib.urlretrieve (src_url, "video.mp4")


# Then upload to AWS S3 Glacier
# Then delete

          client.session.end()
 

# Get entries
listEntries()
