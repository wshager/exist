/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["betterform.ui.select.OptGroup"]){dojo._hasResource["betterform.ui.select.OptGroup"]=true;dojo.provide("betterform.ui.select.OptGroup");dojo.require("dijit._Widget");dojo.require("betterform.ui.ControlValue");dojo.require("dojox.data.dom");dojo.declare("betterform.ui.select.OptGroup",dijit._Widget,{values:"",handleStateChanged:function $DA4K_(_1){var _2=dojo.byId(_1.parentId);if(_1.targetName=="label"&&_2!=undefined){var _3=dojo.byId(dojo.attr(this.domNode.parentNode,"id"));var i;for(i=0;i<_3.length;i++){if(_1.parentId==dojo.attr(_3.options[i],"id")){_3.options[i].text=_1.value;}}}else{if(_1.targetName=="value"&&_2!=undefined){dojo.attr(_2,"value",_1.value);if(dojo.hasClass(this.domNode.parentNode.localName=="select")){var _5=dijit.byId(dojo.attr(this.domNode.parentNode,"id"));if(_5.currentValue==_1.value){_5._handleSetControlValue(_1.value);}}else{console.warn("OptGroup.handleInsert parentNode is not select");}}else{console.warn("OptGroup.handleStateChanged: no action taken for contextInfo: ",_1);}}},handleInsert:function $DA4L_(_6){var _7=document.createElement("option");dojo.addClass(_7,"xfSelectorItem");var _8=_6.generatedIds;var _9=dojo.query(".xfSelectorPrototype",dojo.byId(_6.originalId+"-prototype"))[0];if(_8!=undefined){dojo.attr(_7,"id",_8[_6.prototypeId]);if(_9!=undefined){var _a=_8[dojo.attr(_9,"title")];if(_a!=undefined){dojo.attr(_7,"title",_a);}var _b=_8[dojo.attr(_9,"value")];if(_b!=undefined){dojo.attr(_7,"value",_b);}_7.innerHTML=_9.innerHTML;}}if(_6.label!=undefined){dojo.query(".xfSelectorItem",_7).addContent(_6.label);}if(_6.value!=undefined){dojo.attr(_7,"value",_6.value);}dojo.place(_7,this.domNode,_6.position);},handleDelete:function $DA4M_(_c){var _d=dojo.query(".xfSelectorItem",this.domNode)[_c.position-1];this.domNode.removeChild(_d);}});}