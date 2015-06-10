# DCCD

The DCCD software is an online digital archiving system for dendrochronological data. 
A recent version of this software (system) is  deployed as 'Digital Collaboratory for Cultural Dendrochronology' (DCCD) at [http://dendro.dans.knaw.nl](http://dendro.dans.knaw.nl "DCCD website").
More information about the Digital Collaboratory for Cultural Dendrochronology (DCCD) project can be found here: http://vkc.library.uu.nl/vkc/dendrochronology. 

## Building DCCD from Source
There is no binary distribution so you have to build the application from the sources. 
This comes down to downloading the sources from the code repository and building them. 
because the user interface contains DCCD specific information and some files are also specific for DCCD/DANS. 


### DCCD web application

The DCCD web application (dccd-webui) and the tools depend on the dccd-lib project, which implements the business logic and data models. 
The dccd-lib in turn depends on the dccd-legacy-libs. 

The code building order therefore is:

1. dccd-legacy-libs
2. dccd-lib
3. dccd-webui (dccd-http, dccd-oai) 

#### Build legacy libs

    # cd {devpath}


First you need to install some jar/pom's in your .m2 , because they are difficult to get from a public maven repo. 
Just run the script in the dans-commons project: /dans-commons/repo/install.sh

	# cd dccd-legacy-libs/repo
    # ./install.sh
    
and then build it with maven. 

    # cd ..
	# mvn clean install

#### Build dccd-lib
    



#### Build dccd-webui
Next you need the dccd-webui project repo. 





### DCCD tools (optional)


There are now only two of those ‘extra’ services under development:

For more details read the README files accompanying the source code. 
## Installing DCCD
Explains how to install and deploy the DCCD on a webserver. 
[DCCD Installation Guide](INSTALL.md)

## Developing DCCD
These instructions explain what is needed to develop on the software. 
[DCCD Development Guide](DEVELOPMENT.md)


### Adapting the web application 

## Manuals
The extra DCCD documentation consist of the following:

* User manual [DCCD manual.docx](docs/Manuals/DCCD manual.docx)
* Member management manual [DCCD member management manual.docx](docs/Manuals/DCCD member management manual.docx)