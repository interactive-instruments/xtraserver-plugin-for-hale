# Basic workflow

1. Create a new hale project and import source and target schemas, as usual. Currently, for the source schema you are limited to schemas imported from a PostgreSQL database. For the target schema, a GML 3.2 application schema should be used.
2. Turn on the XtraServer compatibility mode, to be notified when an incompatibility that makes the alignment not automatically translatable to an XtraServer mapping is detected.
3. Import an XtraServer mapping file that matches the given source and target schemas to generate the new alignment.
4. Optionally make changes to the alignment.
5. Export the alignment as an XtraServer mapping file or archive.
