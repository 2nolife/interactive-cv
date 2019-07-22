## Excel template to convert CV into triples ##

Rename `cv-template.xlsm` to `cv-your-name.xlsm` and fill it with your experience and career history.
Export the data as triples into `cv-your-name.nt` file and feed it to the application.

I do not code VBA and this is my best attempt, improvements welcome. No ActiveX controls as they do not work on Mac.

### cv-template.xlsm ###
Template to convert your CV into triples.

* Person page: your skills and other data about you.
* Career page: your career history, all the companies and assignments.
* Export page: export all the data as triples.

### bas and frm files ###
Exported modules and forms to easily upgrade the template to a new version

### Changes ###
* v1: predefined sections and dropdowns, slow when exporting data but gets the job done, works in `Excel for Mac 2011`
