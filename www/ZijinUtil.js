var exec = require('cordova/exec');

exports.printText = function(options, successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'ZijinUtil', 'printText', options);
}
exports.printQRCode = function(options, successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'ZijinUtil', 'printQRCode', [options]);
}

exports.printBlankLine = function(lineHeight, successCallback, errorCallback){
  exec(successCallback, errorCallback, 'ZijinUtil', 'printBlankLine', [{lineHeight: lineHeight}]);
}

exports.releasePrinter = function(){
  exec(null, null,'ZijinUtil', 'releasePrinter', []);
}

exports.scanBarcode = function(successCallback, errorCallback){
  exec(successCallback, errorCallback,'ZijinUtil', 'scanBarcode', []);
}

exports.openScanReceiver = function(successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'ZijinUtil', 'openScanReceiver', []);
}

exports.closeScanReceiver = function() {
  exec(null, null, 'ZijinUtil', 'closeScanReceiver', []);
}

exports.releaseScan = function(){
  exec(null, null,'ZijinUtil', 'releaseScan', []);
}

exports.startInventoryReal = function(successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'ZijinUtil', 'startInventoryReal', []);
}
exports.stopInventoryReal = function() {
  exec(null, null, 'ZijinUtil', 'stopInventoryReal', []);
}
exports.setOutputPower = function (outPower) {
  exec(null, null, 'ZijinUtil', 'setOutputPower', [{outPower: outPower}]);
};
exports.releaseUHF = function(){
  exec(null, null, 'ZijinUtil','releaseUHF',[]);
}

