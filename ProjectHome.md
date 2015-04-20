**jConfig** is a java configuration manager fully customizable with simple plugin, based on xml file.

One of its best features is the capability to notify configuration changes without restarting the application (or restarting the application server in a j2ee environment).

You need Java 1.5+ to use it.


Release 2.3.0 features:
  * added simultaneous notify on different threads for different configuration listeners
  * added cache on configuration readers
  * added the capability to transform a node and its children back to xml

Release 2.2.6 features:
  * support for multiples configuration files
  * support for tracking changes across all the configuration files used
  * support for auto-recover on configuration error once the xml will be changed
  * you can use any custom data holding your configuration info

Third party libraries:
  * log4j
  * cloning (http://mvnrepository.com/artifact/uk.com.robust-it/cloning/1.7.4)


---

**All the releases prior tothe 2.2.6 are incompatible with the new ones**

---


Now you can download a jar with sources contanining an example on how to use jConfig 2.2.6

Just from Eclipse File->import->Existing projects into workspace and then select archive file. The jar contains all the necessary library to work.

It expects the configuration absolute path as input argument. You can add this to your Eclipse launcher for the imported project `"${workspace_loc}\jConfigExample\configuration\myconf.xml"`