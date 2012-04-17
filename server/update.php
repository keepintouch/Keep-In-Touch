<?php
// Each table in the database has a specific definition,
// if the table doesn't match the definition here, then
// we need to modify that table so that it does match it.

$db_tables = array(0=>array("ServerInfo"=>array("CREATE TABLE ServerInfo (Name CHAR(255) NOT NULL, StartupDatetime BIGINT unsigned NOT NULL default '0', Version CHAR(255) NOT NULL, PRIMARY KEY (Name));")),
                   1=>array("Members"=>array("CREATE TABLE Members (ID CHAR(255) NOT NULL, SecureID CHAR(255) NOT NULL, Name CHAR(255) NOT NULL, RSA_PUB_MOD BLOB default '', RSA_PUB_EXP BLOB default '', MemberSince BIGINT unsigned NOT NULL default '0', AllowFriendRequests CHAR(2) NOT NULL DEFAULT 'Y', Public CHAR(2) NOT NULL DEFAULT 'Y', UpdateInterval INT NOT NULL DEFAULT '10', PRIMARY KEY(ID));")),
                   2=>array("Friends"=>array("CREATE TABLE Friends (MemberID CHAR(255) NOT NULL, RequesterID CHAR(255) NOT NULL, AllowHistory CHAR(2) NOT NULL DEFAULT 'Y', FOREIGN KEY (MemberID) REFERENCES Members(ID) ON DELETE CASCADE, FOREIGN KEY (RequesterID) REFERENCES Members(ID) ON DELETE CASCADE, PRIMARY KEY (MemberID, RequesterID));")),
                   3=>array("PendingFriends"=>array("CREATE TABLE PendingFriends (MemberID CHAR(255) NOT NULL, RequesterID CHAR(255) NOT NULL, RequesterMsg CHAR(255) NOT NULL, FOREIGN KEY (MemberID) REFERENCES Members(ID) ON DELETE CASCADE, FOREIGN KEY (RequesterID) REFERENCES Members(ID) ON DELETE CASCADE, PRIMARY KEY(MemberID, RequesterID));")),
                   4=>array("PendingFriendsResponse"=>array("CREATE TABLE PendingFriendsResponse (MemberID CHAR(255) NOT NULL, RequesterID CHAR(255) NOT NULL, MemberMsg CHAR(255) NOT NULL, Accepted CHAR(2) NOT NULL DEFAULT 'N', Encryption CHAR(255) NOT NULL, EncryptionPass BLOB default '', FOREIGN KEY (MemberID) REFERENCES Members(ID) ON DELETE CASCADE, FOREIGN KEY (RequesterID) REFERENCES Members(ID) ON DELETE CASCADE, PRIMARY KEY(MemberID, RequesterID));")),
                   5=>array("RemoveFriends"=>array("CREATE TABLE RemoveFriends (MemberID CHAR(255) NOT NULL, RequesterID CHAR(255) NOT NULL, FOREIGN KEY (MemberID) REFERENCES Members(ID) ON DELETE CASCADE, FOREIGN KEY (RequesterID) REFERENCES Members(ID) ON DELETE CASCADE, PRIMARY KEY(MemberID, RequesterID));")),
                   6=>array("Locations"=>array("CREATE TABLE Locations (Datetime BIGINT unsigned NOT NULL default '0', MemberID CHAR(255) NOT NULL, Encryption CHAR(255) NOT NULL, LatLong BLOB NOT NULL, LocationType CHAR(255) NOT NULL, FOREIGN KEY (MemberID) REFERENCES Members(ID) ON DELETE CASCADE, PRIMARY KEY(Datetime, MemberID));",
                                               "CREATE INDEX DatetimeMemberID_index ON Locations (Datetime, MemberID);)"))
                   );

$db_table_fields = array("ServerInfo"=>array(
                                             "Name"=>array("char(255)", "NO", "PRI", "NULL", ""),
                                             "StartupDatetime"=>array("bigint(20) unsigned", "NO", "", "0", ""),
                                             "Version"=>array("char(255)", "NO", "", "NULL", "")
                                            ),
                         "Friends"=>array(
                                          "MemberID"=>array("char(255)", "NO", "PRI", "NULL", ""),
                                          "RequestID"=>array("char(255)", "NO", "PRI", "NULL", ""),
                                          "AllowHistory"=>array("char(2)", "NO", "", "Y", "")
                                         ),
                         "Members"=>array(
                                          "ID"=>array("char(255)", "NO", "PRI", "NULL", ""),
                                          "SecureID"=>array("char(255)", "NO", "PRI", "NULL", ""),
                                          "Name"=>array("char(255)", "NO", "", "NULL", ""),
                                          "RSA_PUB_MOD"=>array("blob", "YES", "", "NULL", ""),
                                          "RSA_PUB_EXP"=>array("blob", "YES", "", "NULL", ""),
                                          "MemberSince"=>array("bigint(20) unsigned", "NO", "", "0", ""),
                                          "AllowFriendRequests"=>array("char(2)", "NO", "", "Y", ""),
                                          "Public"=>array("char(2)", "NO", "", "Y", ""),
                                          "UpdateInterval"=>array("int(11)", "NO", "", "10", "")
                                         ),
                        );
?>
