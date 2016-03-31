;(function($,$n2){

    var $loggerDiv = null;

    function log(msg) {
        console.log(msg);

        if( $loggerDiv
         && typeof msg === 'string' ){
            var $div = $('<div>')
                .text(msg)
                .appendTo($loggerDiv);
        };
    };

    function runTests(){
        // Load up logger
        $loggerDiv = $('<div>')
            .appendTo( $('body') );

        $n2.cordovaPlugin.echo({
            msg: '12345'
            ,onSuccess: function(msg){
                if( '12345' === msg ){
                    log('echo success: '+msg);
                } else {
                    log('echo error: Unexpected message ('+msg+')');
                };
            }
            ,onError: function(err){
                log('echo error: '+err);
            }
        });

        $n2.cordovaPlugin.getConnectionInfo({
            onSuccess: function(info){
                log('connection name: '+info.name);
            }
        });

        var server = $n2.cordovaCouchbase.getServer();
        var db = server.getDb({
            dbName: 'docs'
        });
        db.getInfo({
            onSuccess: function(info){
                for(var key in info){
                    var value = info[key];
                    log('getInfo '+key+': '+value);
                };
            }
            ,onError: function(err){
                log('error during db getInfo: '+err);
            }
        });
        db.createDocument({
            data: {
                test: 'This is just a test'
            }
            ,onSuccess: function(info){
                log('document created: '+info.id+'/'+info.rev);
                docCreated(info.id);
            }
            ,onError: function(err){
                log('error during document creation: '+err);
            }
        });
        db.listAllDocuments({
            onSuccess: function(docIds){
                for(var i=0,e=docIds.length; i<e; ++i){
                    var docId = docIds[i];
                    log('list all: '+docId);
                };
                allDocIdsReported(docIds);
            }
            ,onError: function(err){
                log('error during listing of all documents: '+err);
            }
        });
        db.getAllDocuments({
            onSuccess: function(docs){
                for(var i=0,e=docs.length; i<e; ++i){
                    var doc = docs[i];
                    log('getAllDocuments: '+doc._id);
                };
            }
            ,onError: function(err){
                log('error during fetching of all documents: '+err);
            }
        });
        var designDoc = db.getDesignDoc({
            ddName: 'atlas'
        });
        designDoc.queryView({
            viewName: 'schemas-root'
            ,onSuccess: function(rows){
                log('*** query results: '+rows.length);
                for(var i=0,e=rows.length; i<e; ++i){
                    var row = rows[i];
                    log('query: '+row.id+'/'+row.key);
                };
            }
            ,onError: function(err){
                log('error during query: '+err);
            }
        });
        designDoc.queryView({
            viewName: 'nunaliit-schema'
            ,startkey: 'module'
            ,endkey: 'module'
            ,onSuccess: function(rows){
                log('*** query (start/end) results: '+rows.length);
                for(var i=0,e=rows.length; i<e; ++i){
                    var row = rows[i];
                    log('query (start/end): '+row.id+'/'+row.key);
                };
            }
            ,onError: function(err){
                log('error during query (start/end): '+err);
            }
        });
        designDoc.queryView({
            viewName: 'nunaliit-schema'
            ,keys: ['module']
            ,onSuccess: function(rows){
                log('*** query (keys) results: '+rows.length);
                for(var i=0,e=rows.length; i<e; ++i){
                    var row = rows[i];
                    log('query (keys): '+row.id+'/'+row.key);
                };
            }
            ,onError: function(err){
                log('error during query (keys): '+err);
            }
        });

        function allDocIdsReported(docIds){
            var someDocIds = [];
            for(var i=0,e=docIds.length; i<e && i<5; ++i){
                var docId = docIds[i];
                someDocIds.push(docId);
            };
            someDocIds.push('abbccddee');

            log('someDocIds.length: '+someDocIds.length);

            db.getDocuments({
                docIds: someDocIds
                ,onSuccess: function(docs){
                    for(var i=0,e=docs.length; i<e; ++i){
                        var doc = docs[i];
                        log('getDocuments: '+doc._id);
                    };
                }
                ,onError: function(err){
                    log('error during getting documents: '+err);
                }
            });
        };

        function docCreated(docId){
            db.getDocumentRevision({
                docId: docId
                ,onSuccess: function(rev){
                    log('document revision: '+rev);
                }
                ,onError: function(err){
                    log('error while fetching document revision: '+err);
                }
            });

            db.getDocument({
                docId: docId
                ,onSuccess: function(doc){
                    log('document fetched: '+doc._id);
                    docFetched(doc);
                }
                ,onError: function(err){
                    log('error while fetching document: '+err);
                }
            });
        };

        function docFetched(doc){
            doc.updated = 'Updated!';
            db.updateDocument({
                data: doc
                ,onSuccess: function(info){
                    log('document updated: '+info.rev);

                    doc._rev = info.rev;
                    docUpdated(doc);
                }
                ,onError: function(err){
                    log('error while updating document: '+err);
                }
            });
        };

        function docUpdated(doc){
            db.deleteDocument({
                data: doc
                ,onSuccess: function(info){
                    log('document deleted: '+info.id+'/'+info.rev);
                }
                ,onError: function(err){
                    log('error while deleting document: '+err);
                }
            });
        };
    };

    function main(config){

        $('.n2loading').remove();

        //runTests();
        new $n2.cordovaLayout.Layout({
            config: config
        });
    };

    function start() {
        nunaliitConfigure({
            configuredFunction: main
        });
    };

    // document.addEventListener('deviceready',start, false);
    start(); // edge your bet for desktop support


})(jQuery, nunaliit2);

