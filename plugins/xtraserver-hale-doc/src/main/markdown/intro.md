# XtraServer plugin for hale

XtraServer is a product of interactive instruments. It is a suite of implementations of various OGC service specifications, e.g. Web Feature Service (WFS) and Web Map Service (WMS).

XtraServer services can base on any application schema according to the Geography Markup Language (GML). For this, a mapping from the GML application schema to the table structure of the underlying database is to be provided in the configuration of the service.

The mapping language of XtraServer is very flexible and can virtually map all GML application schemas to heavily deviating database schemas. For this reason, mappings can be quite complex.

The purpose of this plugin is to

- transform the XtraServer mappings to hale alignments (via Import) and
- generate new XtraServer mapping file easily (via Export)

## Installation requirements

The plug-in is already bundled in the standard hale studio distribution, starting from version 3.4.0.
