/*
Copyright (c) 2016, Geomatics and Cartographic Research Centre, Carleton
University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 - Neither the name of the Geomatics and Cartographic Research Centre,
   Carleton University nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/
;(function($,$n2){
"use strict";

// Localization
var _loc = function(str,args){ return $n2.loc(str,'nunaliit2-cordova',args); };

// ===========================================================
function Configure(options_){

	var options = $n2.extend({
	},options_);

	var configuration = {
		directory: {}
	};

	// Start function
	configuration.start = function(){
		if( configuration.directory.dispatchService ){
			configuration.directory.dispatchService.send('n2.couchConfiguration',{type:'start'});
		};
	};

	// Adjust configuration based on local storage
//	var debugConfiguration = new $n2.debug.DebugConfiguration();
//	if( debugConfiguration.isBadProxyEnabled() ){
//		$n2.couch.setBadProxy(true);
//	};

	// Dispatcher
//	var dispatchLogging = false;
//	if( debugConfiguration.isEventLoggingEnabled() ){
		var dispatchLogging = true;
//	};
	configuration.directory.dispatchService = new $n2.dispatch.Dispatcher({
		logging: dispatchLogging
	});

//	$n2.couchMap.Configure({
       //		dispatchService: configuration.directory.dispatchService
       //	});

	// History monitoring
	configuration.directory.historyMonitor = new $n2.history.Monitor({
		directory: configuration.directory
	});
	configuration.directory.historyTracker = new $n2.history.Tracker({
		directory: configuration.directory
	});
	configuration.directory.history = new $n2.history.History({
		dispatchService: configuration.directory.dispatchService
	});

	// Event translation
	configuration.directory.eventService = new $n2.couchEvents.EventSupport({
		directory: configuration.directory
	});

	// Custom Service
	configuration.directory.customService = new $n2.custom.CustomService({
		directory: configuration.directory
	});

	// Intent Service
	configuration.directory.userIntentService = new $n2.userIntentView.IntentService({
		dispatchService: configuration.directory.dispatchService
	});

 	// Turn off cometd
 	$.cometd = {
 		init: function(){}
 		,subscribe: function(){}
 		,publish: function(){}
 	};

	configuration.couchServer = $n2.cordovaCouchbase.getServer({
		onSuccess: couchInitialized
	});

	function couchInitialized(server) {

		configuration.couchServer = server;
		configuration.directory.couchServer = configuration.couchServer;

		configuration.atlasDb = configuration.couchServer.getDb({dbName:'docs'});
		configuration.atlasDesign = configuration.atlasDb.getDesignDoc({ddName:'atlas'});
		configuration.siteDesign = configuration.atlasDb.getDesignDoc({ddName:'site'}); // not implemented

//		if( options.submissionDbUrl ){
//			configuration.submissionDb = configuration.couchServer.getDb({dbUrl:options.submissionDbUrl});
//		};

		configuration.dataSources = [];

		var couchDbDs = null;
		couchDbDs = new $n2.couchDocument.CouchDocumentSource({
			id: 'main'
			,db: configuration.atlasDb
			,dispatchService: configuration.directory.dispatchService
			,isDefaultDocumentSource: true
		});
		configuration.dataSources.push(couchDbDs);
		configuration.documentSource = couchDbDs;

		// Check browser compliance
		if( $n2.couchHelp
		 && $n2.couchHelp.CheckBrowserCompliance ){
			$n2.couchHelp.CheckBrowserCompliance({
				db: configuration.atlasDb
			});
		};
		configuration.directory.schemaRepository = new $n2.couchSchema.CouchSchemaRepository({
			db: configuration.atlasDb
			,designDoc: configuration.atlasDesign
			,dispatchService: configuration.directory.dispatchService
			,preload: true
			,preloadedCallback: schemasPreloaded
		});
	};

	function schemasPreloaded() {
		configuration.directory.authService = new $n2.cordovaAuth.AuthService({
			onSuccess: authInitialized
			,atlasDb: configuration.atlasDb
			,schemaRepository: configuration.directory.schemaRepository
			,directory: configuration.directory
		});
	};

	function authInitialized() {

		configuration.directory.localizationService = new $n2.couchL10n.LocalizationService({
			db: configuration.atlasDb
	 		,designDoc: configuration.atlasDesign
	 		,dispatchService: configuration.directory.dispatchService
	 	});

//	 	configuration.directory.progressService = new $n2.progress.ProgressServer({
//			url: options.progressServerUrl
//		});
//
//	 	configuration.directory.uploadService = new $n2.upload.Upload({
//			url: options.uploadServerUrl
//			,progressServer: configuration.directory.progressService
//		});
//
//	 	configuration.directory.mailService = new $n2.mail.MailService({
//			url: options.mailServerUrl
//			,dispatchService: configuration.directory.dispatchService
//			,customService: configuration.directory.customService
//		});
//
//		configuration.directory.exportService = new $n2.couchExport.Export({
//			url: options.exportServerUrl
//		});
//
//		configuration.directory.dateService = new $n2.dateService.DateService({
//			url: options.dateServerUrl
//		});

	 	configuration.directory.searchService = new $n2.couchSearch.SearchServer({
			designDoc: configuration.atlasDesign
			,db: configuration.atlasDb
			,dispatchService: configuration.directory.dispatchService
			,customService: configuration.directory.customService
			,dateService: configuration.directory.dateService
		});

//	 	configuration.mediaRelativePath = options.mediaUrl;

	 	configuration.directory.requestService = new $n2.couchRequests({
			documentSource: configuration.documentSource
			,userDb: null // configuration.couchServer.getUserDb()
			,dispatchService: configuration.directory.dispatchService
			,userServerUrl: options.userServerUrl
		});

		configuration.directory.dispatchSupport = new $n2.couchDispatchSupport.DispatchSupport({
			dispatchService: configuration.directory.dispatchService
		});

		configuration.directory.languageService = new $n2.languageSupport.LanguageService({
			directory: configuration.directory
		});

//		configuration.directory.attachmentService = new $n2.couchAttachment.AttachmentService({
//			mediaRelativePath: options.mediaUrl
//		});

		configuration.directory.displayImageSourceFactory = new $n2.couchDisplayBox.DisplayImageSourceFactory({
			documentSource: configuration.documentSource
			,attachmentService: configuration.directory.attachmentService
			,dispatchService: configuration.directory.dispatchService
		});

		// Navigation Service
		configuration.directory.navigationService = new $n2.couchNavigation.NavigationService({
			dispatchService: configuration.directory.dispatchService
			,documentSource: configuration.documentSource
		});

		configuration.directory.showService = new $n2.couchShow.Show({
			db: configuration.atlasDb
			,documentSource: configuration.documentSource
			,requestService: configuration.directory.requestService
			,dispatchService: configuration.directory.dispatchService
			,schemaRepository: configuration.directory.schemaRepository
			,customService: configuration.directory.customService
			,attachmentService: configuration.directory.attachmentService
			,displayImageSourceFactory: configuration.directory.displayImageSourceFactory
		});

		configuration.directory.dialogService = new $n2.couchDialogs.DialogService({
			dispatchService: configuration.directory.dispatchService
			,documentSource: configuration.documentSource
			,searchService: configuration.directory.searchService
			,showService: configuration.directory.showService
			,schemaRepository: configuration.directory.schemaRepository
		});

		configuration.directory.createDocProcess = new $n2.couchRelatedDoc.CreateRelatedDocProcess({
			documentSource: configuration.documentSource
			,schemaRepository: configuration.directory.schemaRepository
			,uploadService: configuration.directory.uploadService
			,showService: configuration.directory.showService
			,authService: configuration.directory.authService
			,dialogService: configuration.directory.dialogService
			,dispatchService: configuration.directory.dispatchService
		});

	 	configuration.directory.schemaEditorService = new $n2.couchEdit.SchemaEditorService({
			documentSource: configuration.documentSource
			,showService: configuration.directory.showService
			,searchService: configuration.directory.searchService
			,dispatchService: configuration.directory.dispatchService
			,dialogService: configuration.directory.dialogService
		});

	 	configuration.directory.editService = new $n2.couchEdit.EditService({
			documentSource: configuration.documentSource
			,schemaRepository: configuration.directory.schemaRepository
			,uploadService: configuration.directory.uploadService
			,showService: configuration.directory.showService
			,authService: configuration.directory.authService
			,dispatchService: configuration.directory.dispatchService
			,searchService: configuration.directory.searchService
			,schemaEditorService: configuration.directory.schemaEditorService
			,customService: configuration.directory.customService
			,dialogService: configuration.directory.dialogService
			,createDocProcess: configuration.directory.createDocProcess
		});
	 	configuration.couchEditor = configuration.directory.editService; // legacy

//	 	configuration.directory.userService = new $n2.couchUser.UserService({
//			userDb: $n2.couch.getUserDb()
//			,configService: configuration.directory.configService
//			,schemaRepository: configuration.directory.schemaRepository
//			,schemaEditorService: configuration.directory.schemaEditorService
//			,userServerUrl: options.userServerUrl
//			,customService: configuration.directory.customService
//		});

	 	configuration.directory.modelService = new $n2.model.Service({
			dispatchService: configuration.directory.dispatchService
		});

	 	configuration.directory.canvasService = new $n2.canvas.Service({
			dispatchService: configuration.directory.dispatchService
		});

	 	configuration.directory.displayService = new $n2.display.Service({
			dispatchService: configuration.directory.dispatchService
		});

	 	configuration.directory.widgetService = new $n2.widgetBasic.Service({
	 		config: configuration
		});

//	 	$n2.mapAndControls.DefaultPopupHtmlFunction = function(opt_){
//	 		var feature = opt_.feature;
//
//	 		if( feature.cluster && feature.cluster.length === 1 ){
//	 			feature = feature.cluster[0];
//	 		};
//
//	 		if( feature.cluster ){
//	 			var clusterSize = feature.cluster.length;
//	 			if( feature.attributes && feature.attributes.count ){
//		 			clusterSize = feature.attributes.count;
//	 			};
//
//				var $tmp = $('<span class="n2_popup"></span>');
//				$tmp.text( _loc('This cluster contains {count} features',{
//					count: clusterSize
//				}) );
//
//		 		var $wrapper = $('<div></div>');
//		 		$wrapper.append($tmp);
//		 		var html = $wrapper.html();
//
//		 		opt_.onSuccess(html);
//
//	 		} else {
//		 		var doc = opt_.feature.data;
//
//		 		var $tmp = $('<span class="n2_popup"></span>');
//		 		configuration.directory.showService.displayBriefDescription($tmp,{},doc);
//
//		 		var $wrapper = $('<div></div>');
//		 		$wrapper.append($tmp);
//		 		var html = $wrapper.html();
//
//		 		opt_.onSuccess(html);
//	 		};
//	 	};

	 	// Set up hover sound
//	 	configuration.directory.hoverSoundService = new $n2.couchSound.HoverSoundService({
//			db: configuration.atlasDb
//			,serviceDirectory: configuration.directory
//	 	});

		// Set up GeoNames service
//		var geoNamesOptions = {};
//		if( window.nunaliit_custom
//		 && window.nunaliit_custom.geoNames ){
//			if( window.nunaliit_custom.geoNames.username ){
//				geoNamesOptions.username = window.nunaliit_custom.geoNames.username;
//			};
//		};
//		configuration.directory.geoNamesService = new $n2.GeoNames.Service(geoNamesOptions);

//		configuration.directory.importProfileService = new $n2.couchImportProfile.ImportProfileService({
//			atlasDb: configuration.atlasDb
//			,atlasDesign: configuration.atlasDesign
//			,schemaRepository: configuration.directory.schemaRepository
//		});

		configuration.directory.documentListService = new $n2.couchDocumentList.DocumentListService({
			atlasDesign: configuration.atlasDesign
			,dispatchService: configuration.directory.dispatchService
		});

//		configuration.directory.simplifiedGeometryService = new $n2.couchSimplifiedGeometries.Service({
//			url: options.simplifiedGeometryServerUrl
//			,atlasDb: configuration.atlasDb
//			,dispatchService: configuration.directory.dispatchService
//			,customService: configuration.directory.customService
//		});

//		if( $n2.tuioClient ){
//			configuration.directory.tuioService = new $n2.tuioClient.TuioService({
//				dispatchService: configuration.directory.dispatchService
//			});
//		};

		// Load help file
		if( configuration.atlasDb ){
			$n2.couchHelp.InstallHelpDocument({
				db: configuration.atlasDb
				,id: 'help.dates'
				,key: 'dates'
			});
		};

		callCustomConfiguration();
	};

	function callCustomConfiguration(){
		if( window
		 && window.nunaliit_custom
		 && typeof(window.nunaliit_custom.configuration) === 'function' ){
			window.nunaliit_custom.configuration(configuration, configurationDone);
		} else {
			configurationDone();
		};
	};

	function configurationDone(){
		// Fix HTML from page
		if( configuration.directory.showService ){
			configuration.directory.showService.fixElementAndChildren( $('body') );
		};

		$n2.log('nunaliit configuration',configuration);
		options.onSuccess(configuration);
	};
};

$n2.cordovaConfiguration = {
	Configure: Configure
};

})(jQuery,nunaliit2);
