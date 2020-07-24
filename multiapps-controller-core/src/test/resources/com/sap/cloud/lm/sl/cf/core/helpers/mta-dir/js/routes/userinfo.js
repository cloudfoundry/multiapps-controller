var express = require('express');
var router = express.Router();
var winston = require('winston');
var userInfoQuery = require('./userinfoQuery');
winston.level = process.env.winston_level || 'error';

router.get('/rest/addressbook/userinfo', function (req, res) {
    userInfoQuery(req.user, req.db, function(error, result) {
        if(error) {
            res.writeHead(500, {'Content-Type': 'application/json'});
            res.end('{}');
        }
        else {
            res.end(JSON.stringify(result, null, 4));
        }
    });
});
module.exports = router;
