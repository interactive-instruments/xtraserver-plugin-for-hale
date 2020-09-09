# Import XtraServer mapping

Via the XtraServer plugin it is possible to transform an XtraServer mapping into a hale alignment.

The first step is to import a database schema as source schema. Open the import dialog for source schema (File -> Import -> Source schema).


![](img/import_source.jpg)


Select the database panel and configure the database connection


![](img/import_source_db.jpg)


and user settings.


![](img/import_source_db_user.jpg)


Then import the target schema (File -> Import -> Target schema). 


![](img/import_target_file.jpg)


Notice: activate the checkbox for Mappable types.


![](img/import_target_mappable-types.jpg)


In the last step import the alignment from file, e.g. XtraSrvConfig_Mapping.inc.xml (File -> Import -> Alignment). Based on this mapping a hale alignment will be created.


![](img/import_alignment.jpg)
