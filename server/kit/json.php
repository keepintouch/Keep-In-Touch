<?php

class JSONData
{
  private $number_of_entries;
  private $result_string;

  // construct
  public function __construct()
  {
     $this->number_of_entries = 0;
     $this->result_string = "[";
  }

  // append entry (array)
  public function append($entry)
  {
    if ($this->number_of_entries > 0)
    { $this->result_string = $this->result_string . ","; }
    $j = json_encode($entry);
    $this->result_string = $this->result_string . $j;
    $this->number_of_entries++;
  }

  // return string of JSON entries
  public function getString()
  {
    return $this->result_string . "]";
  }

  // Clear current result_string
  public function clear()
  {
    $this->result_string = "[";
  }

  // Return array from JSON string
  public function getArray($string)
  {
    return json_decode($string,true);
  }
}

// Example Usage
//$jsonData = new JSONData();
//$a = array();
//$a['name'] = "George";
//$a['phone'] = "12345";
//$a['email'] = "67890";
//$jsonData->append($a);
//$a['name'] = "Phil";
//$a['phone'] = "77777";
//$a['email'] = "99999";
//$jsonData->append($a);
//$final_string = $jsonData->getString();
//print $final_string;
//print "<br><br>\n\n";
//$jsonData = new JSONData();
//foreach ($jsonData->getArray($final_string) as $entry)
//{
//  foreach ($entry as $field_name => $field_value)
//  { print $field_name.": ".$field_value."<br>\n"; }
//  print "<br>\n";
//}
?>
