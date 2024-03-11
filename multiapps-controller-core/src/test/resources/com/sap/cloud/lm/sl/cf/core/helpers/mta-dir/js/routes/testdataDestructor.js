var async = require('async');
var cds = require('cds');
var winston = require('winston');

var BOOK;
var ADDRESS;

winston.level = process.env.winston_level || 'error';

function init(appCallback) {
    if (BOOK && ADDRESS) {
        appCallback();
    } else {
        async.waterfall([
            function (callback) {
                cds.importEntities([ {
                        $entity: "com.sap.xs2.samples::AddressBook.Book",
                        $fields: { // for convenience we add an association from books to addresses
                            addresses: {
                                $association: {
                                    $entity: "com.sap.xs2.samples::AddressBook.Address",
                                    $viaBacklink: "book"
                                }
                            }
                        }
                    },
                    {$entity: "com.sap.xs2.samples::AddressBook.Address"}
                ], callback);
            },
            function (entities, callback) {
                BOOK = entities["com.sap.xs2.samples::AddressBook.Book"];
                ADDRESS = entities["com.sap.xs2.samples::AddressBook.Address"];
                callback(null);
            }
        ], function (error) {
            appCallback(error);
        });
    }
}

function testdataDestructor(dbConnection, securityContext, appCallback) {
    var conn = null;
    async.waterfall([
        init,
        function connect(cb) {
            // reuse connection attached to HTTP request
            cds.$getTransaction(dbConnection, cb);
        },
        function store(tx, cb) {
            conn = tx;
            cb(null);
        },
        // TODO: this if inefficient; convert $find/$discardAll to 2x $queries instead
        function findAllBooks(cb) {
            conn.$find(BOOK, {}, cb);
        },
        function deleteAllBooks(books, cb) {
            if (!securityContext.checkLocalScope('Delete')) {
                return cb(new Error('Insufficient permissions. You do not have the required Delete scope. '
                    +'Create a role based on the Editor role template and assign the role to a group which contains your user!'));
            }
            conn.$discardAll(books, cb);
        },
        function release(cb) {
            conn.$close();
            cb(null);
        }
    ], appCallback);
}

module.exports = testdataDestructor;
