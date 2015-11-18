var duplicate = false;
jQuery( document ).ready( function()
{
	showHideUserGroup();

	checkValueIsExist( "name", "validateProgramStage.action", {id:getFieldValue('programId'), programStageId:getFieldValue('id')});	
	
	jQuery("#availableList").dhisAjaxSelect({
		source: "../dhis-web-commons-ajax-json/getDataElements.action?domain=patient",
		iterator: "dataElements",
		connectedTo: 'selectedDataElementsValidator',
		handler: function(item) {
			var option = jQuery("<option />");
			option.text( item.name );
			option.attr( "value", item.id );
			
			if( item.optionSet == "true"){
				option.attr( "valuetype", "optionset" );
			}
			else{
				option.attr( "valuetype", item.type );
			}
			
			var flag = false;
			jQuery("#selectedList").find("tr").each( function( k, selectedItem ){ 
				if(selectedItem.id == item.id )
				{
					flag = true;
					return;
				}
			});
			
			if(!flag) return option;
		}
	});
});
