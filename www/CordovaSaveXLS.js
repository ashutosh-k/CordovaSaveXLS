var exec = require('cordova/exec');

exports.savexls = function(arg0, success, error) {
    exec(success, error, "CordovaSaveXLS", "saveXLS", [arg0]);
};

