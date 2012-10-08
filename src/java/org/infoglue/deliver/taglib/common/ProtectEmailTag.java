package org.infoglue.deliver.taglib.common;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspException;

import org.apache.commons.codec.binary.Base64;
import org.infoglue.deliver.taglib.component.ComponentLogicTag;

public class ProtectEmailTag extends ComponentLogicTag
{
	private static final long serialVersionUID = 5835981227146633985L;
	
	private static final String DEFAULTLABEL = "Skyddad e-post";
	private static final String DEFAULTPREFIX = "IGEncodedEmailAddress";
	private static final String DEFAULTSPANCLASS = "igProtectedEmailClass";
	private static final String DEFAULTSPANWRAPPERCLASS = "igProtectedEmailWrapperClass";
	
	private String value;
	private String label;
	private String prefix;
	private String spanClass;
	private String wrapperSpanClass;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public int doEndTag() throws JspException
	{
		if (label == null || "".equals(label.trim()))
		{
			label = DEFAULTLABEL;
		}
		if (prefix == null || "".equals(prefix.trim()))
		{
			prefix = DEFAULTPREFIX;
		}
		if (spanClass == null || "".equals(spanClass.trim()))
		{
			spanClass = DEFAULTSPANCLASS;
		}
		if (wrapperSpanClass == null || "".equals(wrapperSpanClass.trim()))
		{
			wrapperSpanClass = DEFAULTSPANWRAPPERCLASS;
		}
		
		String noJSClass = "nonjs";
		
		String script = "<script type=\"text/script\" src=\"/infoglueDeliverWorking/script/jquery/jquery-1.2.6.min.js\"></script>\n" +
				"<script type=\"text/javascript\">/* <![CDATA[ */" +
				"var keyStr = \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\";" +
				"function decode64(input) {var output = \"\";var chr1, chr2, chr3 = \"\";var enc1, enc2, enc3, enc4 = \"\";var i = 0;" +
				"var base64test = /[^A-Za-z0-9\\+\\/\\=]/g;if (base64test.exec(input)) {}" +
				"input = input.replace(/[^A-Za-z0-9\\+\\/\\=]/g, \"\");" +
				"do {enc1 = keyStr.indexOf(input.charAt(i++));enc2 = keyStr.indexOf(input.charAt(i++));" +
				"enc3 = keyStr.indexOf(input.charAt(i++));enc4 = keyStr.indexOf(input.charAt(i++));" +
				"chr1 = (enc1 << 2) | (enc2 >> 4);chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);chr3 = ((enc3 & 3) << 6) | enc4;" +
				"output = output + String.fromCharCode(chr1);if (enc3 != 64) {output = output + String.fromCharCode(chr2);}" +
				"if (enc4 != 64) {output = output + String.fromCharCode(chr3);} chr1 = chr2 = chr3 = \"\";enc1 = enc2 = enc3 = enc4 = \"\";" +
				"} while (i < input.length);return unescape(output);}\n " +
				
				"function dcodeEmail(){" +
                "$(\"span."+spanClass+"\").each(function (index,elem) {" +
                "$(elem).text($(elem).text().replace(/"+prefix+"_([ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=]*)_"+prefix+"/g,function(a,b){return decode64(b);}));" +
                "$(elem).replaceWith($(elem).text());});" +
                "$('a[href^=\"mailto:\"]').each(function (index,elem) {" +
                "$(elem).text($(elem).text().replace(/"+prefix+"_([ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=]*)_"+prefix+"/g,function(a,b){return decode64(b);}));" +
                "elem.href = elem.href.replace(/"+prefix+"_([ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=]*)_"+prefix+"/g,function(a,b){return decode64(b);});});}\n" +
                "$(document).ready(dcodeEmail);" +
                "$(document).ready(function() {" +
                "$(\"body\").removeClass(\"" + noJSClass + "\");});" +
                "/* ]]> */</script>";
		
		String style = "<style type=\"text/css\">" +
					"/* Email security */" +
					"." + noJSClass + " a[href^=\"mailto:\"] {text-decoration: none; color: black;}" +
					"." + noJSClass + " ."+spanClass+" {display: none;}" +
					"." + noJSClass + " ."+wrapperSpanClass+":before {content: \"" + label + "\";}" +
					"</style>";

		Collection htmlHeadItems = getController().getDeliveryContext().getHtmlHeadItems();
		if (!htmlHeadItems.contains(script))
		{
			htmlHeadItems.add(script);
		}
		if (!htmlHeadItems.contains(style))
		{
			htmlHeadItems.add(style);
		}

		Pattern pattern = Pattern.compile("(mailto:){0,1}([A-Za-z0-9][A-Za-z0-9\\._%+-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4})");//  (mailto:){0,1}(\\w+[\\.\\w+]+@\\w+\\.\\w+)	
		Matcher matcher = pattern.matcher(value);
		StringBuffer sb = new StringBuffer();
		while (matcher.find())
		{
			if (matcher.group(1) != null) // mailto-link
			{
				String email = matcher.group(2);
				String encryptedEmail = new String(Base64.encodeBase64(email.getBytes()));
				matcher.appendReplacement(sb, "mailto:" + prefix + "_" +  encryptedEmail + "_" + prefix);
			}
			else
			{
				String email = matcher.group(2);
				String encryptedEmail = new String(Base64.encodeBase64(email.getBytes()));
				matcher.appendReplacement(sb, "<span class=\"" + wrapperSpanClass + "\"><span class=\"" + spanClass + "\">" + prefix + "_" +  encryptedEmail + "_" + prefix + "</span></span>");
			}
		}
		matcher.appendTail(sb);
		produceResult(sb);
		
		this.value = null;
		this.label = null;
		this.prefix = null;
		this.spanClass = null;
		this.wrapperSpanClass = null;
		
		return EVAL_PAGE;
	}
	

	public void setValue(String value) throws JspException
	{
		this.value = evaluateString("secureEmail", "value", value);
	}
	public void setPrefix(String prefix) throws JspException
	{
		this.prefix = evaluateString("secureEmail", "prefix", prefix);
	}
	public void setSpanClass(String spanClass) throws JspException
	{
		this.spanClass = evaluateString("secureEmail", "spanClass", spanClass);
	}
	public void setWrapperSpanClass(String wrapperSpanClass) throws JspException
	{
		this.wrapperSpanClass = evaluateString("secureEmail", "wrapperSpanClass", wrapperSpanClass);
	}
}
