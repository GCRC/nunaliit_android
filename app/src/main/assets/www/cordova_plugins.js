/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/

cordova.define('cordova/plugin_list', function(require, exports, module) {
    module.exports = [
        {
            "file": "plugins/org.apache.cordova.camera/www/CameraConstants.js",
            "id": "org.apache.cordova.camera.Camera",
            "clobbers": [
                "Camera"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.camera/www/CameraPopoverOptions.js",
            "id": "org.apache.cordova.camera.CameraPopoverOptions",
            "clobbers": [
                "CameraPopoverOptions"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.camera/www/Camera.js",
            "id": "org.apache.cordova.camera.camera",
            "clobbers": [
                "navigator.camera"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.camera/www/CameraPopoverHandle.js",
            "id": "org.apache.cordova.camera.CameraPopoverHandle",
            "clobbers": [
                "CameraPopoverHandle"
            ]
        },



        {
            "file": "plugins/org.apache.cordova.file/www/DirectoryEntry.js",
            "id": "org.apache.cordova.file.DirectoryEntry",
            "clobbers": [
                "window.DirectoryEntry"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/DirectoryReader.js",
            "id": "org.apache.cordova.file.DirectoryReader",
            "clobbers": [
                "window.DirectoryReader"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/Entry.js",
            "id": "org.apache.cordova.file.Entry",
            "clobbers": [
                "window.Entry"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/File.js",
            "id": "org.apache.cordova.file.File",
            "clobbers": [
                "window.File"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileEntry.js",
            "id": "org.apache.cordova.file.FileEntry",
            "clobbers": [
                "window.FileEntry"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileError.js",
            "id": "org.apache.cordova.file.FileError",
            "clobbers": [
                "window.FileError"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileReader.js",
            "id": "org.apache.cordova.file.FileReader",
            "clobbers": [
                "window.FileReader"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileSystem.js",
            "id": "org.apache.cordova.file.FileSystem",
            "clobbers": [
                "window.FileSystem"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileUploadOptions.js",
            "id": "org.apache.cordova.file.FileUploadOptions",
            "clobbers": [
                "window.FileUploadOptions"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileUploadResult.js",
            "id": "org.apache.cordova.file.FileUploadResult",
            "clobbers": [
                "window.FileUploadResult"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/FileWriter.js",
            "id": "org.apache.cordova.file.FileWriter",
            "clobbers": [
                "window.FileWriter"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/Flags.js",
            "id": "org.apache.cordova.file.Flags",
            "clobbers": [
                "window.Flags"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/LocalFileSystem.js",
            "id": "org.apache.cordova.file.LocalFileSystem",
            "clobbers": [
                "window.LocalFileSystem"
            ],
            "merges": [
                "window"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/Metadata.js",
            "id": "org.apache.cordova.file.Metadata",
            "clobbers": [
                "window.Metadata"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/ProgressEvent.js",
            "id": "org.apache.cordova.file.ProgressEvent",
            "clobbers": [
                "window.ProgressEvent"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/requestFileSystem.js",
            "id": "org.apache.cordova.file.requestFileSystem",
            "clobbers": [
                "window.requestFileSystem"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/resolveLocalFileSystemURI.js",
            "id": "org.apache.cordova.file.resolveLocalFileSystemURI",
            "merges": [
                "window"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/fileSystems.js",
            "id": "org.apache.cordova.file.fileSystems",
            "merges": [
                "window"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/fileSystemPaths.js",
            "id": "org.apache.cordova.file.fileSystemPaths",
            "merges": [
                "window"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.file/www/fileSystems-roots.js",
            "id": "org.apache.cordova.file.fileSystems-roots",
            "merges": [
                "window"
            ]
        },



        {
            "file": "plugins/org.apache.cordova.mediacapture/www/CaptureAudioOptions.js",
            "id": "org.apache.cordova.mediacapture.CaptureAudioOptions",
            "clobbers": [
                "CaptureAudioOptions"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/CaptureImageOptions.js",
            "id": "org.apache.cordova.mediacapture.CaptureImageOptions",
            "clobbers": [
                "CaptureImageOptions"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/CaptureVideoOptions.js",
            "id": "org.apache.cordova.mediacapture.CaptureVideoOptions",
            "clobbers": [
                "CaptureVideoOptions"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/CaptureError.js",
            "id": "org.apache.cordova.mediacapture.CaptureError",
            "clobbers": [
                "CaptureError"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/MediaFileData.js",
            "id": "org.apache.cordova.mediacapture.MediaFileData",
            "clobbers": [
                "MediaFileData"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/MediaFile.js",
            "id": "org.apache.cordova.mediacapture.MediaFile",
            "clobbers": [
                "MediaFile"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/capture.js",
            "id": "org.apache.cordova.mediacapture.capture",
            "clobbers": [
                "navigator.device.capture"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.mediacapture/www/helpers.js",
            "id": "org.apache.cordova.mediacapture.helpers"
        },



        {
            "file": "plugins/org.apache.cordova.media/www/Media.js",
            "id": "org.apache.cordova.media.Media",
            "clobbers": [
                "window.Media"
            ]
        },
        {
            "file": "plugins/org.apache.cordova.media/www/MediaError.js",
            "id": "org.apache.cordova.media.MediaError",
            "clobbers": [
                "window.MediaError"
            ]
        },



        {
            "file": "plugins/io.github.pwlin.cordova.plugins.fileopener2/www/plugins.FileOpener2.js",
            "id": "io.github.pwlin.cordova.plugins.fileopener2.FileOpener2",
            "clobbers": [
                "window.cordova.plugins.fileOpener2"
            ]
        },

        {
            "file": "plugins/net.spantree.searchbar/www/searchbar.js",
            "id": "net.spantree.searchbar.SearchBar",
            "clobbers": [
                "cordova.searchbar"
            ]
        },
    ];
});
