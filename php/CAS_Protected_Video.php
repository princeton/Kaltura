<?php

/**
*  This code example shows how to authenticate a user with CAS
*  before generating a Kaltura Session that will allow playback
*  of a video requiring a Kaltura Session.
*
*  Dependencies: 
*          Kaltura client Library http://www.kaltura.com/api_v3/testme/client-libs.php
*          CAS client for PHP http://www.jasig.org/cas/client-integration
*          EmbedCodeGenerator https://github.com/kaltura/EmbedCodeGenerator
*
*  In the code below you must supply values for:
*     $CAS_server
*     $CAS_port
*     PARTNER_ID
*     USER_SECRET
*     $user
*     ENTRY_ID
*     UICONF_ID
*/

// Authenticate the user via CAS ...

// import phpCAS lib
include_once('CAS.php');
 
// Set the CAS specific log file
//phpCAS::setDebug('/NAShomes/publichtmls/mycpanel/ratliff/tmp/phpCAS.log');
 
// initialize phpCAS
$CAS_server = 'The hostname of your CAS server';
$CAS_port = The port that your CAS server listens on;
phpCAS::client(CAS_VERSION_2_0,$CAS_server,$CAS_port,'cas');
 
// no SSL validation for the CAS server
phpCAS::setNoCasServerValidation();
 
// force CAS authentication
phpCAS::forceAuthentication();
?>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Kaltura Session Limited Audio Playback</title>
</head>

<body>
<?php

/**
* Now that the user has been authenticated, we generate the required Kaltura Session
**/

// Load the Kaltura PHP library required to generate the session.  Download from http://www.kaltura.com/api_v3/testme/client-libs.php
require_once "client/KalturaClient.php";

// Your Kaltura credentials
define("PARTNER_ID", "Obtain from KMC settings tab");
define("USER_SECRET", "Obtain from KMC settings tab");

// The username this code should use when accessing the KMC
$user = "bcstaff";  // Whatever user id you want to set (This is according to your system deployment)

// The entry id of the video that will be played
define("ENTRY_ID", "Obtain from KMC content tab");

// The id of the player to be used
define("UICONF_ID", "Obtain from KMC studio tab");

echo '<h1>Your netID: ' . phpCAS::getUser() . '</h1>';

//Create a Kaltura Session string
$conf = new KalturaConfiguration(PARTNER_ID);
$client = new KalturaClient($conf);

//This is the session start function signature: start($secret, $userId = "", $type = 0, $partnerId = -1, $expiry = 86400, $privileges = "")
//It is important that we pass the special permission "sview:ENTRY_ID" to provide access for the specific entry we want to play.
//This generated KS (Kaltura Session) will then be rendered to the page if the user has permissions to access the full video.
//If the user doesn't have permissions to access the full video, we'll not render a KS in the flashvars, 
//and Kaltura will only stream the preview part of the video as defined in the access control profile.
$session = $client->session->start(USER_SECRET, $user, KalturaSessionType::USER, PARTNER_ID, 86400, 'sview:'.ENTRY_ID);

if (!isset($session)) {
	die("Could not establish Kaltura session with OLD session credentials. Please verify that you are using valid Kaltura partner credentials.");
}

$client->setKs($session);
?>

<!-- Generate the embed code using the Kaltura Embed Code Generator JavaScript library.  The library can be downloaded here
         https://github.com/kaltura/EmbedCodeGenerator -->
<script src="EmbedCodeGenerator/dist/KalturaEmbedCodeGenerator.js"></script>
<script>
var gen = new kEmbedCodeGenerator({
    partnerId: "<?php echo PARTNER_ID; ?>",
    uiConfId: "<?php echo UICONF_ID; ?>",
    entryId: "<?php echo ENTRY_ID; ?>",
	flashVars: {ks: "<?php echo $session; ?>"}
});
document.write(gen.getCode());
</script>

<p> Kaltura Session =
<pre><?php echo $session; ?></pre>
</p>
</body>
</html>
