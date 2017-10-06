sap.ui.jsview("jds.tree", {

	getControllerName : function() {
		return "jds.tree";
	},

	createContent : function(oController) {
        var backendURL = "/rest/addressbook/tree"
	    var oModel = new sap.ui.model.json.JSONModel(backendURL, false);
        var restErrorMessage = "Something has gone wrong while accessing the REST service at " + backendURL + ". Please check whether the node.js application " +
                 "is up and running. Depending on your runtime either execute 'cf logs node-hello-world-backend --recent' or 'xs logs node-hello-world-backend --recent'."

        oModel.attachParseError(function(oControlEvent) {
            alert(restErrorMessage);
        });

        oModel.attachRequestFailed(function(oControlEvent) {
            alert(restErrorMessage);
        });

        var oTable = new sap.ui.table.TreeTable({
            id : "AddressBookOverview",
            columns: [
                new sap.ui.table.Column({label: "Name", template: "name"}),
                new sap.ui.table.Column({label: "City", template: "city"}),
                new sap.ui.table.Column({label: "Phone", template: "phone"})
            ],
            visibleRowCount : 20,
            width : "100%",
            selectionMode : sap.ui.table.SelectionMode.Single,
            selectionBehavior : sap.ui.table.SelectionBehavior.Row
        });

        oTable.setModel(oModel);
        var oSorter = new sap.ui.model.Sorter("name");
        oTable.bindRows("/books", oSorter);

        // button to generate more data
        var oBtn = new sap.ui.commons.Button({text: "Create Data",
            press: function() {
                var aData = jQuery.ajax({
                    type : "GET",
                    contentType : "application/json",
                    url : "/rest/addressbook/testdata",
                    dataType : "json",
                    async: false, 
                    success : function(data, textStatus, jqXHR) {
                        oModel.loadData("/rest/addressbook/tree");
                    },
                    statusCode: {
                        401 : function() {
                            location.reload();
                        }
                    }
                });
            }
        });
        // button to delete all data
        var oDeleteBtn = new sap.ui.commons.Button({text: "Delete All Data (with Auth. Check)",
            press: function() {
                var bData = jQuery.ajax({
                    type : "GET",
                    contentType : "application/json",
                    url : "/rest/addressbook/testdataDestructor",
                    dataType : "json",
                    async: false, 
                    success : function(data, textStatus, jqXHR) {
                        oModel.loadData("/rest/addressbook/tree");
                    },
                    statusCode: {
                        401 : function() {
                            location.reload();
                        },
	                    403: function() {
	                    	alert('You are not authorized to Delete Data! To remedy this, your user needs to get the Delete scope. '
	                              +'Thus, create a role based on the Editor role template and assign the role to a group which contains your user!');
	                    }
                    }
                });
            }
        });
        oTable.setToolbar(new sap.ui.commons.Toolbar({items: [oBtn, oDeleteBtn]}));

        var oPanel = new sap.ui.commons.Panel().setText("Address Books").addContent(oTable);
        return oPanel;
	}
});
