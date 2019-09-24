var exec = require('cordova/exec');

module.exports = {
    get: function(success, error, options) {
        console.log("executando o croqui plugin");
        exec(success, error, "BigrsCroqui", "new_activity", [options]);
    }
};