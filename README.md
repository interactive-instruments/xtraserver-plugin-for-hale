XtraServer Plugin for hale studio
=================================

[XtraServer](https://www.interactive-instruments.de/xtraserver/) is a suite of implementations of various OGC service specifications, e.g. Web Feature Service (WFS) and Web Map Service (WMS).

XtraServer services can be based on any application schema according to the Geography Markup Language (GML). To set up such a service, a mapping from the GML application schema to the table structure of the underlying database has to be provided. The mapping language of XtraServer is very flexible and can virtually map all GML application schemas to heavily deviating database schemas. For this reason, mappings can be quite complex.

The purpose of this plugin is to import and export [hale studio](https://www.wetransform.to/products/halestudio/) alignments from and to XtraServer mappings. That allows to create and edit XtraServer mappings with the graphical editor of hale studio.


## Installation

Currently the *XtraServer Plugin for hale studio* can only be used with recent [development builds](https://builds.wetransform.to/job/hale/job/hale~publish(master)/) of hale studio.

The *XtraServer Plugin for hale studio* can be installed directly from the hale plugin manager:

- Open **Help -> Install New Software**
- Select **Work with -> XtraServer Plugin for hale studio**
- Check **XtraServer support for hale**
- Click **Next**
- Click **Finish**
- Click **Install anyway** when the **Security Warning** comes up (This should not be necessary anymore in a future version)
- Click **Restart Now**

## Update

To update the *XtraServer Plugin for hale studio*, just open **Help -> Check for Updates**. If a newer version is available, you can proceed to install it.
