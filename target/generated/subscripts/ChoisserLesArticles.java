package subscripts;

//---------------------------------------------------------------------------------------
//	    _  _____ ____     ____                           _             
//	   / \|_   _/ ___|   / ___| ___ _ __   ___ _ __ __ _| |_ ___  _ __ 
//	  / _ \ | | \___ \  | |  _ / _ \ '_ \ / _ \ '__/ _` | __/ _ \| '__|
//	 / ___ \| |  ___) | | |_| |  __/ | | |  __/ | | (_| | || (_) | |   
//	/_/   \_\_| |____/   \____|\___|_| |_|\___|_|  \__,_|\__\___/|_|   
//
//---------------------------------------------------------------------------------------
//	/!\ Warning /!\
//	This class was automatically generated by ATS Script Generator (ver. 3.2.8)
//	You may lose modifications if you edit this file manually !
//---------------------------------------------------------------------------------------

import com.ats.script.*;
import com.ats.script.actions.*;
import com.ats.script.actions.neoload.*;
import com.ats.script.actions.performance.*;
import com.ats.script.actions.performance.octoperf.*;
import com.ats.executor.ActionTestScript;
import com.ats.generator.objects.Cartesian;
import com.ats.generator.objects.mouse.Mouse;
import com.ats.generator.variables.ConditionalValue;
import com.ats.tools.Operators;
import org.openqa.selenium.Keys;

public class ChoisserLesArticles extends ActionTestScript{

	/**
	* Test Name : <b>subscripts.ChoisserLesArticles</b>
	* Generated at : <b>13 août 2024, 15:30:06</b>
	* Script created at : 24 juil. 2024, 15:22:37
	* 
	* @author	AishwaryaMANORE
	*/

	// -----------------[ ATS script info ]----------------- //

	public static final java.lang.String ATS_TEST_ID = "";
	public static final java.lang.String ATS_TEST_NAME = "subscripts.ChoisserLesArticles";
	public static final java.lang.String ATS_AUTHOR_NAME = "AishwaryaMANORE";
	public static final java.lang.String ATS_DESCRIPTION = "";
	public static final java.lang.String ATS_PREREQUISITES = "";
	public static final java.lang.String ATS_EXTERNAL_ID = "";
	public static final java.lang.String ATS_GROUPS = "";
	public static final java.lang.String ATS_PROJECT_ID = "com.functional.QAFOOD(0.0.1)";
	public static final java.lang.String ATS_PROJECT_UUID = "6F5263FF-9AB2-03EE-6227-DAD693CB9170";
	public static final java.lang.String ATS_LOGS_TYPE = "term";
	public static final java.lang.String ATS_MEMO = "term";
	public static final java.lang.String ATS_PRE_PROCESSING = "#PROJECT_PRE_PROCESSING#";
	public static final java.lang.String ATS_POST_PROCESSING = "#PROJECT_POST_PROCESSING#";

	// -----------------[ Constructors ]----------------- //

	public ChoisserLesArticles(){super();}
	public ChoisserLesArticles(com.ats.executor.ActionTestScript sc){super(sc);}

	// -----------------[ Overrides ]----------------- //

	@java.lang.Override
	protected ScriptHeader getScriptHeader(){return new ScriptHeader(ATS_PROJECT_UUID, ATS_PROJECT_ID, ATS_TEST_NAME, ATS_TEST_ID, ATS_AUTHOR_NAME, ATS_DESCRIPTION, ATS_PREREQUISITES, ATS_EXTERNAL_ID, ATS_MEMO, ATS_PRE_PROCESSING, ATS_POST_PROCESSING, ATS_GROUPS, ATS_LOGS_TYPE);}

	// -----------------[ Test ]----------------- //

	public void testMain(){
		TcChoisserLesArticles();
	}

	@org.testng.annotations.Test
	public void TcChoisserLesArticles(){

		//   ---< Variables >---   //

		com.ats.generator.variables.Variable propertyVar1 = var("propertyVar1");
		com.ats.generator.variables.Variable FoodItem = var("FoodItem", clv("12 wings"));

		//   ---< Actions >---   //

		exec(0,new ActionMouseKey(this, 0, 24, 0, el(clv("2"), false, "A", prp(Operators.EQUAL, "data-title", clv(prm("FoodItem")))), ms(Mouse.CLICK)));
		exec(1,new ActionProperty(this, 1, 0, 0, el(clv("0"), false, "H2", prp(Operators.EQUAL, "text", clv("Vos param\u00E8tres de commande"))), "text", propertyVar1));
		if(condition(ConditionalValue.VALUE_EQUALS, 2, propertyVar1, clv("Vos param\u00E8tres de commande"))) exec(2,new ActionCallscript(this, clv("subscripts.ParametreDeCommande"), false, false, clv("assets:///data/data_construction_de_commande/ParametreDeCommande.csv"), null));
		exec(3,new ActionMouseKey(this, 0, 2, 0, el(clv("0"), false, "INPUT", prp(Operators.EQUAL, "type", clv("button")), prp(Operators.EQUAL, "value", clv("+"))), ms(Mouse.CLICK)));
		exec(4,new ActionMouseKey(this, 1, 0, 6, el(el(clv("0"), false, "FORM", prp(Operators.EQUAL, "id", clv("fooditem-details"))), clv("0"), false, "LABEL", prp(Operators.EQUAL, "class", clv("checkbox-container")), prp(Operators.EQUAL, "text", clv(prm("Toppings")))), ms(Mouse.CLICK)));
		exec(5,new ActionMouseKey(this, 0, 0, 0, el(clv("0"), false, "A", prp(Operators.EQUAL, "href", clv("javascript:void(0);"))), ms(Mouse.CLICK)));
	}
}