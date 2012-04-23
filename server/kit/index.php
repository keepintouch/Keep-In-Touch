<?php
  $server_version = "1.0";
  $config_file = "config.php";
  include_once "json.php";
  include_once "update.php";

  // TODO: Pretty up the ERROR messages!

  // Make sure our config file is readable so that we can include it
  if (is_readable($config_file))
  {
    include_once $config_file;
  }
  else
  {
    print "ERROR: '".$config_file."' file is not readable, try doing a 'chmod 644 ".$config_file."' at the command line.\n";
    exit(0);
  }

  // DEBUGGING PURPOSES....
  //$debugging = 1;
  $restrict_access = "";
  if ($restrict_access != "")
  {
    if (strcmp($_SERVER['REMOTE_ADDR'],$restrict_access) != 0)
    {
      print "&Pi;";
      exit(0);
    }
  }

  // If the database hasn't been setup, then show the user the setup screen
  if ($mysql_server == "" || $mysql_db == "" || $mysql_username == "" || $mysql_password == "")
  {
    include "setup.php";
  }

  // Try and connect to the MySQL server
  $connection = @mysql_connect($mysql_server, $mysql_username, $mysql_password);
  if (!$connection)
  {
    print "ERROR: Database not accessible!<br>MySQL ERROR: ".mysql_error();
    // TODO: Write up the "database form to setup the database access"
    exit(0);
  }
  
  // Try and open the MySQL database
  $mysqldb = @mysql_select_db($mysql_db, $connection);
  if (!$mysqldb)
  {
    print "ERROR: Database not accessible!<br>MySQL ERROR: ".mysql_error();
    // TODO: Write up the "database form to setup the database access"
    exit(0);
  }

  // Check and make sure our tables exist in the database
  $db_tables_created = 0;
  $db_tables_updated = 0;
  $table_creation_info = "";
  for($i=0; $i < count($db_tables); $i++)
  {
    foreach ($db_tables[$i] as $k=>$v)
    {
      $q = @mysql_query("DESCRIBE $k");
      if (!$q)
      {
        $table_creation_info .= "<tr><td>Creating Table '$k':</td><td><font color=\"#00AAAA\"><b>OK</b></font></td></tr>\n";
        for ($j=0; $j < count($v); $j++)
        {
          $q = @mysql_query($v[$j]);
          $db_tables_created++;
          //print $v[$j]."<br>";
        }
      }
      else
      {
        // TODO: If $server_version doesn't match server version stored in database
        //       then check the table definitions and update them if necessary and
        //       then update the version number in the database...
      }
    }
  }
  if ($db_tables_created > 0)
  {
    $server_name = $_POST['server_name'];
    if (trim($server_name) == "")
    { $server_name = $_SERVER['SERVER_NAME']; }

    $time = time();
    $q = @mysql_query("INSERT INTO ServerInfo SET Name=\"".mysql_real_escape_string($server_name)."\", StartupDatetime=\"$time\", Version=\"$server_version\"");
    print "&nbsp;<br>\n";
    print "<div style=\"display: block; width: 600; margin: 0 auto; padding: 5px; background-color: #FFFF88; -webkit-border-radius: 10px; -moz-border-radius: 10px; border-radius: 10px; border: 1px black solid;\">\n";
    print "<table border=\"0\" align=\"center\">\n";
    print $table_creation_info;
    print "</table>\n";
    print "&nbsp;<br><center><b>All database tables were successfully created!</b></center><br>";
    $perms = fileperms("config.php");
    if (($perms & 0x0010) || ($perms & 0x0002)) // Group or Other "writable"
    {
      print "<center><font color=\"#FF0000\">WARNING - Fix config.php permissions for read-only access:<br> &nbsp; &nbsp; &nbsp; <span style=\"font-family: courier new,courier;\"><font size=\"-1\"><b>chmod 644 config.php</b><br>&nbsp;</font></span></font></center>";
    }
    print "</div>\n";
    exit(0);
  }

  // ************** CHECK POINT 1
  // At this point, we've connected to our MySQL server, and our tables all
  // exist.

  // Now let's see if our ServerInfo table is filled in
  $q = mysql_query("SELECT * FROM ServerInfo") or die("Select from ServerInfo Failed");
  $rowcount = mysql_num_rows($q);
  if ($rowcount > 0)
  {
    $rs = mysql_fetch_assoc($q);
    $server_info = array("Name"=>$rs['Name'], "StartupDatetime"=>$rs['StartupDatetime'], "Version"=>$rs['Version'], "MaxHistory"=>$rs['MaxHistory']);
    //print_r($server_info);
  }
  else
  {
    // TODO: Setup form to enter in ServerInfo before continuing....
    print "ERROR: ServerInfo hasn't been entered";
    exit(0);
  }

  // ************** CHECK POINT 2
  // All of our server info is good, so let's start parsing the data passed to us

  $data = urldecode($_POST['data']);
  # If Debugging, use GET instead
  if ($debugging == 1)
  {
    $data = $_GET['data'];
  }
  # If Logging
  if (strlen($logging) > 0)
  {
    $fh = fopen($logging, 'a');
    fwrite($fh, time()." - ".$_SERVER['REMOTE_ADDR']." - ".$data."\n");
    fclose($fh);
  }
  if (strlen($data) == 0)
  {
    if ($_GET['s'] == "1")
    { print "Server Configured Successfully!"; }
    else
    { print "-!er!-"; }
    exit(0);
  }

  //printf $data;    //Enable for debugging data coming in
  // Loop through each entry we received
  $jsonIncoming = new JSONData();
  $jsonOutgoing = new JSONData();
  foreach ($jsonIncoming->getArray($data) as $entry)
  {
    // Parse cmd
    // 0 = command
    // 1 = ID 
    // 2-X = options for command
    $option_cmd = "";
    $option_id = "";
    $member_id = "";
    if (array_key_exists("cmd", $entry) && array_key_exists("id", $entry))
    {
      $option_cmd = $entry["cmd"];
      $option_id = $entry["id"];
    }
    else
    {
      $jsonOutgoing->append(array("response"=>"0","msg"=>"Command or ID Not supplied, auth failed!"));
      print $jsonOutgoing->getString();
      exit(0);
    }

    if ($option_cmd == "introduce")
    {
      // TODO: We want the server to look at the ID that was sent, so that if a new client
      // wants to use it's old ID number, then we'll recreate the entry in the Members
      // table without generating an md5 hash!
      $required_fields = array("name", "rsa_pub_mod", "rsa_pub_exp", "allowfriendrequests", "public", "updateinterval");
      $fields_ok = 1;
      foreach ($required_fields as $field)
      { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
      if ($fields_ok == 1)
      {
        $option_name = $entry["name"];
        $option_rsa_pub_mod = $entry["rsa_pub_mod"];
        $option_rsa_pub_exp = $entry["rsa_pub_exp"];
        $option_allowfriendrequests = $entry["allowfriendrequests"];
        $option_public = $entry["public"];
        $option_updateinterval = $entry["updateinterval"];
        $md5hash_secure = md5(strval(time()).$option_name);
        $md5hash = md5($option_rsa_pub_mod);  // By using RSA_PUB_MOD, Member ID will alway stay the same
        $q = mysql_query("SELECT ID, SecureID FROM Members WHERE ID=\"".$md5hash."\"");
        if (mysql_num_rows($q) == 0)
        {
          $q = mysql_query("INSERT INTO Members SET ID=\"".mysql_real_escape_string($md5hash)."\", SecureID=\"".mysql_real_escape_string($md5hash_secure)."\", Name=\"".mysql_real_escape_string($option_name)."\", RSA_PUB_MOD=\"".mysql_real_escape_string($option_rsa_pub_mod)."\", RSA_PUB_EXP=\"".mysql_real_escape_string($option_rsa_pub_exp)."\", MemberSince=UNIX_TIMESTAMP(), AllowFriendRequests=\"".mysql_real_escape_string($option_allowfriendrequests)."\", Public=\"".mysql_real_escape_string($option_public)."\", UpdateInterval=".mysql_real_escape_string(strval($option_updateinterval))."") or die("INSERT into Members Failed!");
          $jsonOutgoing->append(array("response"=>"1","msg"=>"Account Successfully created on server."));
          $jsonOutgoing->append(array("cmd"=>"introduce_response","MemberID"=>$md5hash,"SecureID"=>$md5hash_secure));
        }
        else
        {
          $jsonOutgoing->append(array("response"=>"1","msg"=>"Account Already Exists on Server! Welcome back!"));
          $rs = mysql_fetch_assoc($q);
          $jsonOutgoing->append(array("cmd"=>"introduce_response","MemberID"=>$rs['ID'],"SecureID"=>$rs['SecureID']));
          $option_id = $rs['SecureID'];
        }
      }
      else
      {
        $jsonOutgoing->append(array("response"=>"0","msg"=>$option_cmd."-Not enough options received!"));
        print $jsonOutgoing->getString();
        exit(0);
      }
    }
    else
    {
      // Before we parse the command, we need to verify the ID (make sure the user exists
      // in our database)
      $q = mysql_query("SELECT ID FROM Members WHERE SecureID=\"".mysql_real_escape_string($option_id)."\"") or die("Select From Members Failed");
      if (mysql_num_rows($q) < 1)
      {
        $jsonOutgoing->append(array("response"=>"0","msg"=>"Authorization failed!"));
        print $jsonOutgoing->getString();
        exit(0);
      }
      else
      {
        $rsMI = mysql_fetch_assoc($q);
        $member_id = $rsMI['ID'];
      }

      // If we got here, then the user has been authenticated via their ID
      if ($option_cmd == "query_settings(id)")
      {
  
      }
      else if ($option_cmd == "query_location")
      {
        // Accepts: id,user1,[user2],[user3],etc.
        // Note: if user1 = "ALL_FRIENDS", then all friends are returned
        $everything_ok = 1;
        $q = @mysql_query("SELECT * FROM Friends WHERE MemberID=\"".mysql_real_escape_string($member_id)."\" OR RequesterID=\"".mysql_real_escape_string($member_id)."\"");
        if ($q)
        {
          while($rs = mysql_fetch_assoc($q))
          {
            $friendID = $rs['MemberID'];
            if (strcmp($friendID, $member_id) == 0)
            { $friendID = $rs['RequesterID']; }
            $q2 = @mysql_query("SELECT * FROM Locations WHERE MemberID=\"".$friendID."\" ORDER BY datetime DESC limit 1");
            if ($q2)
            {
              if (mysql_num_rows($q2) > 0)
              {
                $rs2 = mysql_fetch_assoc($q2);
                $jsonOutgoing->append(array("cmd"=>"query_location_response",
                                            "datetime"=>$rs2['Datetime'],
                                            "memberid"=>$rs2['MemberID'],
                                            "encryption"=>$rs2['Encryption'],
                                            "latlon"=>$rs2['LatLong'],
                                            "locationtype"=>$rs2['LocationType']
                                            ));
              }
            }
            else
            { $everything_ok = 0; }
          }
        }
        else
        { $everything_ok = 0; }

        if ($everything_ok == 1)
        { $jsonOutgoing->append(array("response"=>"1","msg"=>"Query Location Successful")); }
        else
        { $jsonOutgoing->append(array("response"=>"0","msg"=>"Query Location Failed")); }
      }
      else if ($option_cmd == "query_history")
      {
        $required_fields = array("memberid", "starttime", "endtime");
        $fields_ok = 1;
        foreach ($required_fields as $field)
        { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
        if ($fields_ok == 1)
        {
          $option_memberid = $entry["memberid"];
          $option_starttime = $entry["starttime"];
          $option_endtime = $entry["endtime"];

          $okay_to_view_history = 0;
          if (strcmp($option_memberid, $member_id) == 0)
          { $okay_to_view_history = 1; }
          else
          {
            $qF = @mysql_query("SELECT AllowHistory FROM Friends WHERE (MemberID=\"".mysql_real_escape_string($member_id)."\" AND RequesterID=\"".mysql_real_escape_string($option_memberid)."\") OR (MemberID=\"".mysql_real_escape_string($option_memberid)."\" AND RequesterID=\"".mysql_real_escape_string($member_id)."\")");
            if ($qF)
            {
              if (mysql_num_rows($qF) > 0) // We have a friend relationship, continue
              {
                $rsF = mysql_fetch_assoc($qF);
                if ($rsF['AllowHistory'] == "Y")
                {
                   $okay_to_view_history = 1;
                }
                else
                {
                  $jsonOutgoing->append(array("response"=>"0","msg"=>"History Request Failed - Friend Doesn't Allow History Viewing"));
                }
              }
              else
              {
                $jsonOutgoing->append(array("response"=>"0","msg"=>"History Request Failed - No Friend Relationship"));
              }
            }
            else
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"History Request Failed - Friend Relationship Lookup Failed"));
            }
          }

          if ($okay_to_view_history == 1)
          {
            $q = @mysql_query("SELECT ID FROM Members WHERE ID=\"".mysql_real_escape_string($option_memberid)."\"");
            if ($q)
            {
              if (mysql_num_rows($q) > 0)
              {
                $q = @mysql_query("SELECT Datetime, Encryption, LatLong, LocationType FROM Locations WHERE MemberID=\"".mysql_real_escape_string($option_memberid)."\" AND Datetime>=".mysql_real_escape_string($option_starttime)." AND Datetime<=".mysql_real_escape_string($option_endtime)." ORDER BY Datetime DESC");
                if ($q)
                {
                  while($rs = mysql_fetch_assoc($q))
                  {
                    $jsonOutgoing->append(array("cmd"=>"query_history_response",
                                                "datetime"=>$rs['Datetime'],
                                                "encryption"=>$rs['Encryption'],
                                                "latlon"=>$rs['LatLong'],
                                                "locationtype"=>$rs['LocationType']
                                                ));
                  }
                  $jsonOutgoing->append(array("response"=>"1","msg"=>"History Request Successful"));
                }
                else
                { $jsonOutgoing->append(array("response"=>"0","msg"=>"History Request Failed - Database Problem")); }
              }
              else
              {
                // Nothing to return, but still okay
                $jsonOutgoing->append(array("response"=>"1","msg"=>"History Request Successful"));
              }
            }
            else
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"History Request Failed - No Friends with that ID"));
            }
          }
        }
      }
      else if ($option_cmd == "update_location")
      {
        $required_fields = array("datetime", "encryption", "latlon", "locationtype");
        $fields_ok = 1;
        foreach ($required_fields as $field)
        { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
        if ($fields_ok == 1)
        {
          $option_datetime = $entry["datetime"];
          $option_encryption = $entry["encryption"];
          $option_latlong = $entry["latlon"];
          $option_locationtype = $entry["locationtype"];
          $q = @mysql_query("INSERT INTO Locations SET Datetime=\"".mysql_real_escape_string($option_datetime)."\", MemberID=\"".mysql_real_escape_string($member_id)."\", Encryption=\"".mysql_real_escape_string($option_encryption)."\", LatLong=\"".mysql_real_escape_string($option_latlong)."\", LocationType=\"".mysql_real_escape_string($option_locationtype)."\"");
          if ($q)
          {
            $jsonOutgoing->append(array("response"=>"1","msg"=>"Location Update Successful"));
          }
          else
          {
            $q = mysql_query("SELECT Datetime FROM Locations WHERE Datetime=\"".mysql_real_escape_string($option_datetime)."\" AND MemberID=\"".mysql_real_escape_string($member_id)."\"") or die("Select from Locations Failed");
            if (mysql_num_rows($q) > 0)
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"Location Update Failed - Duplicate Location Time Entry"));
            }
            else
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"Location Update Failed - Couldn't update Location"));
            }
          }
        }
        else
        {
          print "ERROR\nNot enough options received.";
        }
      }
      else if ($option_cmd == "update_settings")
      {
        $required_fields = array("name", "allowfriendrequests", "public", "updateinterval");
        $fields_ok = 1;
        foreach ($required_fields as $field)
        { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
        if ($fields_ok == 1)
        {
          $option_name = $entry["name"];
          $option_allowfriendrequests = $entry["allowfriendrequests"];
          $option_public = $entry["public"];
          $option_updateinterval = $entry["updateinterval"];
          $q = mysql_query("UPDATE Members SET Name=\"".mysql_real_escape_string($option_name)."\", AllowFriendRequests=\"".mysql_real_escape_string($option_allowfriendrequests)."\", Public=\"".mysql_real_escape_string($option_public)."\", UpdateInterval=".mysql_real_escape_string(strval($option_updateinterval))." WHERE SecureID=\"".mysql_real_escape_string($option_id)."\"") or die("Update Members Failed!");
          $jsonOutgoing->append(array("response"=>"1","msg"=>"Settings Update Successful"));

        }
      }
      else if ($option_cmd == "friend_request")
      {
        $required_fields = array("userTo", "msg", "encryption", "encryptionpass");
        $fields_ok = 1;
        foreach ($required_fields as $field)
        { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
        if ($fields_ok == 1)
        {
          $option_userto = $entry["userTo"];
          $option_msg = $entry["msg"];
          $option_encryption = $entry["encryption"];
          $option_encryptionpass = $entry["encryptionpass"];

          $q = @mysql_query("SELECT AllowFriendRequests FROM Members WHERE ID=\"".mysql_real_escape_string($option_userto)."\"");
          if ($q)
          {
            if (mysql_num_rows($q) > 0)
            {
              $rs = mysql_fetch_assoc($q);
              if ($rs['AllowFriendRequests'] == "Y")
              {
                $q2 = @mysql_query("INSERT INTO PendingFriends SET MemberID=\"".mysql_real_escape_string($option_userto)."\", RequesterID=\"".mysql_real_escape_string($member_id)."\", RequesterMsg=\"".mysql_real_escape_string($option_msg)."\", Encryption=\"".mysql_real_escape_string($option_encryption)."\", EncryptionPass=\"".mysql_real_escape_string($option_encryptionpass)."\"");
                if ($q2)
                {
                  $jsonOutgoing->append(array("response"=>"1","msg"=>"Friend Request Successful"));
                }
                else
                {
                  $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Request Failed - DB Insert Failed"));
                }
              }
              else
              {
                $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Request Failed - Member not accepting friend requests"));
              }
            }
            else
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Request Failed - Couldn't find member info"));
            }
          }
          else
          {
            $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Request Failed - DB Select Failed"));
          }
        }
        else
        {
          $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Request Failed - Required fields missing"));
        }
      }
      else if ($option_cmd == "friend_response")
      {
        $required_fields = array("userTo", "msg", "allowhistory", "accepted", "encryption", "encryptionpass");
        $fields_ok = 1;
        foreach ($required_fields as $field)
        { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
        if ($fields_ok == 1)
        {
          $option_userto = $entry["userTo"];
          $option_msg = $entry["msg"];
          $option_allowhistory = $entry["allowhistory"];
          $option_accepted = $entry["accepted"];
          $option_encryption = $entry["encryption"];
          $option_encryptionpass = $entry["encryptionpass"];
          if ($option_accepted == "Y")
          {
            $q = @mysql_query("INSERT INTO PendingFriendsResponse SET MemberID=\"".mysql_real_escape_string($member_id)."\", RequesterID=\"".mysql_real_escape_string($option_userto)."\", MemberMsg=\"".mysql_real_escape_string($option_msg)."\", Accepted=\"Y\", Encryption=\"".mysql_real_escape_string($option_encryption)."\", EncryptionPass=\"".mysql_real_escape_string($option_encryptionpass)."\"");
            if ($q)
            {
              $q2 = @mysql_query("INSERT INTO Friends SET MemberID=\"".mysql_real_escape_string($member_id)."\", RequesterID=\"".mysql_real_escape_string($option_userto)."\", AllowHistory=\"".mysql_real_escape_string($option_allowhistory)."\"");
              if ($q2)
              {
                $jsonOutgoing->append(array("response"=>"1","msg"=>"Friend Response Successful"));
              }
              else
              {
                $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Response Failed - Couldn't setup friend relationship"));
              }
            }
            else
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Response Failed - Couldn't insert pending friend response"));
            }
          }
          else
          {
            // If not accepted, then just do nothing
            $jsonOutgoing->append(array("response"=>"1","msg"=>"Friend Response Successful"));
          }
        }
        else
        {
          $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Response Failed - Required fields missing"));
        }
      }
      else if ($option_cmd == "friend_delete")
      {
        $required_fields = array("userTo");
        $fields_ok = 1;
        foreach ($required_fields as $field)
        { if (!array_key_exists($field, $entry)) { $fields_ok = 0; } }
        if ($fields_ok == 1)
        {
          $option_userto = $entry["userTo"];
          $q = @mysql_query("INSERT INTO RemoveFriends SET MemberID=\"".mysql_real_escape_string($option_userto)."\", RequesterID=\"".mysql_real_escape_string($member_id)."\"");
          if ($q)
          {
            $q2 = @mysql_query("DELETE FROM Friends WHERE (MemberID=\"".mysql_real_escape_string($member_id)."\" AND RequesterID=\"".mysql_real_escape_string($option_userto)."\") OR (MemberID=\"".mysql_real_escape_string($option_userto)."\" AND RequesterID=\"".mysql_real_escape_string($member_id)."\")");
            if ($q2)
            {
              $jsonOutgoing->append(array("response"=>"1","msg"=>"Friend Delete Successful"));
            }
            else
            {
              $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Response Failed - Couldn't delete friend relationship"));
            }
          }
          else
          {
            $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Response Failed - Couldn't insert Remove Friend entry"));
          }
        } 
        else
        {
          $jsonOutgoing->append(array("response"=>"0","msg"=>"Friend Delete Failed - Required fields missing"));
        }
      }
      else if ($option_cmd == "member_list")
      {
        $q = @mysql_query("SELECT ID, Name, MemberSince, RSA_PUB_MOD, RSA_PUB_EXP FROM Members WHERE Public=\"Y\"");
        if ($q)
        {
          if (mysql_num_rows($q) > 0)
          {
            while($rs = mysql_fetch_assoc($q))
            {
              $qF = @mysql_query("SELECT MemberID FROM Friends WHERE (MemberID=\"".mysql_real_escape_string($member_id)."\" AND RequesterID=\"".mysql_real_escape_string($rs['ID'])."\") OR (MemberID=\"".mysql_real_escape_string($rs['ID'])."\" AND RequesterID=\"".mysql_real_escape_string($member_id)."\")");
              if ($qF)
              {
                if (mysql_num_rows($qF) == 0) // If we didn't get a friend match, then send back a result
                {                            // We only want to send back members that member_id isn't already friends with
                  $jsonOutgoing->append(array("cmd"=>"member_list_response",
                                              "name"=>$rs['Name'],
                                              "memberid"=>$rs['ID'],
                                              "membersince"=>$rs['MemberSince'],
                                              "rsa_pub_mod"=>$rs['RSA_PUB_MOD'],
                                              "rsa_pub_exp"=>$rs['RSA_PUB_EXP'],
                                              ));
                }
              }
            }
          }
          $jsonOutgoing->append(array("response"=>"1","msg"=>"Member List Request Successful"));
        }
        else
        {
          $jsonOutgoing->append(array("response"=>"0","msg"=>"Members List Request Failed - Query Failed"));
        }
      }
      else if ($option_cmd == "poll")
      {
        // Let's check and see if there are any pending friend requests
        $q = @mysql_query("SELECT RequesterID, RequesterMsg, Encryption, EncryptionPass FROM PendingFriends WHERE MemberID=\"".mysql_real_escape_string($member_id)."\"");
        if ($q)
        {
          if (mysql_num_rows($q) > 0)
          {
            while($rs = mysql_fetch_assoc($q))
            {
              $q2 = @mysql_query("SELECT Name, RSA_PUB_MOD, RSA_PUB_EXP FROM Members WHERE ID=\"".mysql_real_escape_string($rs['RequesterID'])."\"");
              if ($q2)
              {
                if (mysql_num_rows($q2) > 0)
                {
                  $rs2 = mysql_fetch_assoc($q2);
                  $jsonOutgoing->append(array("cmd"=>"poll_response",
                                              "task"=>"friend_request",
                                              "data1"=>$rs['RequesterID'],
                                              "data2"=>$rs2['Name'],
                                              "data3"=>$rs['RequesterMsg'],
                                              "data4"=>$rs2['RSA_PUB_MOD'],
                                              "data5"=>$rs2['RSA_PUB_EXP'],
                                              "data6"=>$rs['Encryption'],
                                              "data7"=>$rs['EncryptionPass'],
                                              ));
                  $q3 = @mysql_query("DELETE FROM PendingFriends WHERE MemberID=\"".mysql_real_escape_string($member_id)."\" AND RequesterID=\"".mysql_real_escape_string($rs['RequesterID'])."\"");
                }
              }
            }
          }
        }
        // Let's check and see if there are any pending friend responses
        $q = @mysql_query("SELECT * FROM PendingFriendsResponse WHERE RequesterID=\"".mysql_real_escape_string($member_id)."\"");
        if ($q)
        {
          if (mysql_num_rows($q) > 0)
          {
            while($rs = mysql_fetch_assoc($q))
            {
              $q2 = @mysql_query("SELECT Name, RSA_PUB_MOD, RSA_PUB_EXP FROM Members WHERE ID=\"".mysql_real_escape_string($rs['MemberID'])."\"");
              if ($q2)
              {
                if (mysql_num_rows($q2) > 0)
                {
                  $rs2 = mysql_fetch_assoc($q2);
                  $jsonOutgoing->append(array("cmd"=>"poll_response",
                                              "task"=>"friend_response",
                                              "data1"=>$rs['MemberID'],
                                              "data2"=>$rs2['Name'],
                                              "data3"=>$rs2['RSA_PUB_MOD'],
                                              "data4"=>$rs2['RSA_PUB_EXP'],
                                              "data5"=>$rs['MemberMsg'],
                                              "data6"=>$rs['Encryption'],
                                              "data7"=>$rs['EncryptionPass'],
                                              ));
                  $q3 = @mysql_query("DELETE FROM PendingFriendsResponse WHERE MemberID=\"".mysql_real_escape_string($rs['MemberID'])."\" AND RequesterID=\"".mysql_real_escape_string($member_id)."\"");
                }
              }
            }
          }
        }
        // Let's check and see if there are any pending unfriend requests
        $q = @mysql_query("SELECT * FROM RemoveFriends WHERE MemberID=\"".mysql_real_escape_string($member_id)."\"");
        if ($q)
        {
          if (mysql_num_rows($q) > 0)
          {
            while($rs = mysql_fetch_assoc($q))
            {
              $jsonOutgoing->append(array("cmd"=>"poll_response",
                                          "task"=>"friend_delete",
                                          "data1"=>$rs['RequesterID'],
                                          ));
              $q3 = @mysql_query("DELETE FROM RemoveFriends WHERE MemberID=\"".mysql_real_escape_string($rs['MemberID'])."\" AND RequesterID=\"".mysql_real_escape_string($rs['RequesterID'])."\"");
            }
          }
        }
        $jsonOutgoing->append(array("response"=>"1","msg"=>"Poll Successful"));
      }
      else
      {
        $jsonOutgoing->append(array("response"=>"0","msg"=>"Unknown command"));
        print $jsonOutgoing->getString();
        // TODO: Probably should syslog whatever was sent to the server, since it wasn't valid
        exit(0);
      }
    }
  }
  // Send back all our responses as a single JSON object
  print $jsonOutgoing->getString();

  // For testing:
  // https://server.name.here/kit/index.php?data=[{"cmd":"query_location","id":"SECURE_ID_HERE"}]
  @mysql_close($connection);
?>
