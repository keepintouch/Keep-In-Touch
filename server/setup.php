<?php
// This script handles the initial setup of the server and is called by
// the index.php script if necessary.

$no_errors = 1;
$warning = "";
$db_ok = 1;
if ($mysql_server == "" || $mysql_db == "" || $mysql_username == "" || $mysql_password == "")
{
  $show_input_box = 1;
  $server_name = $_SERVER['SERVER_NAME'];
  $my_server = "localhost";
  $my_database = "kit_db";
  $my_username = "kit";
  $my_password = "";
  $table_entries = "";
  if (!is_writable("config.php"))
  {
      $db_ok = 0;
      $table_entries .= "<tr><td>config.php is writable:</td><td><font color=\"#FF0000\"><b>FAILED</b></font></td></tr>\n";
      $table_entries .= "<tr><td>&nbsp;</td><td>Fix with: <span style=\"font-family: courier new,courier;\"><font size=\"-1\"><b>chmod 666 config.php</b><br>&nbsp;</font></span></td></tr>\n";
  }
  else
  {
      $table_entries .= "<tr><td>config.php is writable:</td><td><font color=\"#00AAAA\"><b>OK</b></font></td></tr>\n";
  }

  if (isSet($_POST['submit']))
  {
    $server_name = $_POST['server_name'];
    $my_server = $_POST['my_server'];
    $my_database = $_POST['my_database'];
    $my_username = $_POST['my_username'];
    $my_password = $_POST['my_password'];
    // Try and connect to the MySQL server
    $connection = @mysql_connect($my_server, $my_username, $my_password);
    if (!$connection)
    {
      $db_ok = 0;
      $table_entries .= "<tr><td>Server Connection:</td><td><font color=\"#FF0000\"><b>FAILED</b></font></td></tr>\n";
      $table_entries .= "<tr><td>Database Access:</td><td><font color=\"#FF0000\"><b>FAILED</b></font></td></tr>\n";
    }
    else
    {
      $table_entries .= "<tr><td>Server Connection:</td><td><font color=\"#00AAAA\"><b>OK</b></font></td></tr>\n";
      // Try and open the MySQL database
      $mysqldb = @mysql_select_db($my_database, $connection);
      if (!$mysqldb)
      {
        $q = mysql_query("CREATE DATABASE ".mysql_real_escape_string($my_database));
        if ($q)
        {
          $table_entries .= "<tr><td>$my_database Database Access:</td><td><font color=\"#00AAAA\"><b>OK</b></font></td></tr>\n";
        }
        else
        {
          $db_ok = 0;
          $table_entries .= "<tr><td>$my_database Database Access:</td><td><font color=\"#FF0000\"><b>FAILED</b></font></td></tr>\n";
        }
      }
      else
      {
        $table_entries .= "<tr><td>$my_database Database Access:</td><td><font color=\"#00AAAA\"><b>OK</b></font></td></tr>\n";
      }
    }
    // If our test DB connection was okay, then let's save our config.php
    
    if ($db_ok == 1)
    {
      $show_input_box = 0;
      $new_config = "";
      // Save our new config.php
      $mysql_server = $my_server;
      $mysql_db = $my_database;
      $mysql_username = $my_username;
      $mysql_password = $my_password;
      $lines = file("config.php");
      foreach ($lines as $line)
      {
        $fields = preg_split("/=/", $line);
        if (trim($fields[0]) == "\$mysql_server")
        { $new_config .= "\$mysql_server = \"".addslashes($my_server)."\";\n"; }
        elseif (trim($fields[0]) == "\$mysql_db")
        { $new_config .= "\$mysql_db = \"".addslashes($my_database)."\";\n"; }
        elseif (trim($fields[0]) == "\$mysql_username")
        { $new_config .= "\$mysql_username = \"".addslashes($my_username)."\";\n"; }
        elseif (trim($fields[0]) == "\$mysql_password")
        { $new_config .= "\$mysql_password = \"".addslashes($my_password)."\";\n"; }
        else
        { $new_config .= $line; }
      }
      $fh = fopen("config.php", 'w');
      fwrite($fh, $new_config);
      fclose($fh);

      if (!@chmod("config.php", 0644))
      {
        $warning = "<font color=\"#FF0000\">WARNING - Fix config.php permissions for read-only access:<br> &nbsp; &nbsp; &nbsp; <span style=\"font-family: courier new,courier;\"><font size=\"-1\"><b>chmod 644 config.php</b><br>&nbsp;</font></span></font>";
      }
    }
    else
    {
      $no_errors = 0;
    }
  }

if ($show_input_box == 1)
{ $no_errors = 0; }

if ($table_entries != "")
{
print <<<END
  &nbsp;<br>
  <div style="display: block; width: 600; margin: 0 auto; padding: 5px; background-color: #FFFF88; -webkit-border-radius: 10px; -moz-border-radius: 10px; border-radius: 10px; border: 1px black solid;">
  <table border="0" align="center">
  $table_entries
  </table>
END;
if ($no_errors == 1)
{
  print "&nbsp;<br><center>$warning<center>";
  print "&nbsp;<br>&nbsp;<br><center><a href=\"index.php?s=1\">CLICK HERE TO FINISH THE SETUP!</a></center><br>";
}
print "  </div>\n";
}

if ($show_input_box == 1)
{
print <<<END
  &nbsp;<br>&nbsp;<br>
  <div style="display: block; width: 600; margin: 0 auto; padding: 5px; background-color: #FFFFFF; -webkit-border-radius: 10px; -moz-border-radius: 10px; border-radius: 10px; border: 1px black solid;"><center><font size="+3">INITIAL SETUP</font><br>
  <form method="post">
  <table border="0" align="center">
  <tr><td>Server Name:</td><td><input type="text" name="server_name" value="$server_name"></td></tr>
  <tr><td colspan="2"><hr></td></tr>
  <tr><td>MySQL Server:</td><td><input type="text" name="my_server" value="$my_server"></td></tr>
  <tr><td>MySQL Database:</td><td><input type="text" name="my_database" value="$my_database"></td></tr>
  <tr><td>MySQL Username:</td><td><input type="text" name="my_username" value="$my_username"></td></tr>
  <tr><td>MySQL Password:</td><td><input type="text" name="my_password" value="$my_password"></td></tr>
  </table>
  <input type="submit" name="submit" value="Save">
  </form>
  </div>
  <center>
  &nbsp;<br>&nbsp;
  NOTE: Make sure the database has already been created on the MySQL server:<br>
  <span style="font-family: courier new,courier;"><font size="-1"><b>GRANT ALL ON $my_database.* TO $my_username@localhost IDENTIFIED BY 'PUT_A_SECURE_PASSWORD_HERE';</b></font></span>
  </center>
END;
exit(0);
}
}
else
{
  print "LOOKS LIKE THE SERVER IS ALREADY CONFIGURED!";
}
