sap.ui.jsview("user.info", {

	getControllerName : function() {
		return "user.info"
	},

	createContent : function(oController) {
        var backendURL = "/rest/addressbook/userinfo"
	    var oModel = new sap.ui.model.json.JSONModel(backendURL, false)
        var restErrorMessage = "Something has gone wrong while accessing the REST service at " + backendURL + ". Please check whether the node.js application " +
                 "is up and running. Depending on your runtime either execute 'cf logs node-hello-world-backend --recent' or 'xs logs node-hello-world-backend --recent'."

        oModel.attachRequestFailed(function(oControlEvent) {
            /**
             * if the session is invalid the page will be
             * reloaded and the user will be redirected to the login page
             */
            if(oControlEvent.mParameters && oControlEvent.mParameters.statusCode === 401) {
                location.reload();
            }
            else {
                alert(restErrorMessage);
            }
        })
        sap.ui.getCore().setModel(oModel)
        oModel.loadData("/rest/addressbook/userinfo")

        var oLayout = new sap.ui.commons.layout.MatrixLayout({
            id : "matrix1",
            layoutFixed : false
        })

        userNameLabel = new sap.ui.commons.Label({
            text : 'User ID',
            design : sap.ui.commons.LabelDesign.Bold
        })

        userNameValue = new sap.ui.commons.TextView({
            text : "{/hdbCurrentUser/0/APPLICATION_USER}"
        })

        firstNameLabel = new sap.ui.commons.Label({
            text : 'First name',
            design : sap.ui.commons.LabelDesign.Bold
        })

        firstNameValue = new sap.ui.commons.TextView({
            text : "{/user/name/givenName}"
        })

        lastNameLabel = new sap.ui.commons.Label({
            text : 'Last name',
            design : sap.ui.commons.LabelDesign.Bold
        })

        lastNameValue = new sap.ui.commons.TextView({
            text : "{/user/name/familyName}"
        })

        technicalUserLabel = new sap.ui.commons.Label({
            text : 'Technical DB user',
            design : sap.ui.commons.LabelDesign.Bold
        })

        technicalUserValue = new sap.ui.commons.TextView({
            text : "{/hdbCurrentUser/0/CURRENT_USER}"
        })

        oLayout.createRow(userNameLabel, userNameValue)
        oLayout.createRow(firstNameLabel, firstNameValue)
        oLayout.createRow(lastNameLabel, lastNameValue)
        oLayout.createRow(technicalUserLabel, technicalUserValue)
        return oLayout
	}
});
