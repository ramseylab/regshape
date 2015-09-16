<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
  "http://java.sun.com/products/javahelp/helpset_1_0.dtd" 
  [
  <!ENTITY % AppConfig SYSTEM "../config/AppConfig.dtd">
  %AppConfig;
  ]>

<helpset version="1.0">
  <title>&appName; Help</title>

  <maps>
    <homeID>top</homeID>
    <mapref location="AppHelp.jhm" />
  </maps>

  <view>
    <name>TOC</name>
    <label>Table of Contents</label>
    <type>javax.help.TOCView</type>
    <data>AppHelpTOC.xml</data>
  </view>

</helpset>
