function userInfoQuery(externalUserInfo, dbConnection, callback) {
    responseObject = {};
    //normally one would select from DUMMY but this currently not possible with HDI RTCs
    dbConnection.exec("SELECT TOP 1 CURRENT_USER, SESSION_USER, SESSION_CONTEXT('XS_APPLICATIONUSER') as APPLICATION_USER FROM \"com.sap.xs2.samples::AddressBook.Book\"", function (error, rows) {
        if (error) {
            callback(error, null);
        }
        else {
            responseObject.hdbCurrentUser = rows;
            responseObject.user = externalUserInfo;
            callback(null, responseObject);
        }
    });
}
module.exports = userInfoQuery;
