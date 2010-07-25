/*
 Copyright 2007, Lucid Technics, LLC.

 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 except in compliance with the License. You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in
 writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 specific language governing permissions and limitations under the License.
*/

package airlift.generator;

import org.apache.commons.lang.StringUtils;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.util.Map;

import javax.lang.model.element.Element;

public class JavaScriptGenerator
   extends Generator
{
	public String comment = "This JavaScript code has been generated by airlift. Do not modify this code.";

	public void generate(String _appName,
				String _directory,
				Element _element,
				DomainObjectModel _domainObjectModel,
				Map<String, DomainObjectModel> _elementNameToDomainObjectModelMap)
	{
		String generatedString = generateValidationObject(_domainObjectModel);
		String fileName =  "javascript/airlift/validation/domain/" + _domainObjectModel.getClassName() + "Validator.js";
		writeResourceFile(fileName, _directory, fileName, generatedString, _element);

		generatedString = generateDao(_domainObjectModel);
		fileName =  "javascript/airlift/dao/" + _domainObjectModel.getClassName() + ".js";
		writeResourceFile(fileName, _directory, fileName, generatedString, _element);

		generatedString = generateActiveRecord(_domainObjectModel);
		fileName =  "javascript/airlift/activerecord/" + _domainObjectModel.getClassName() + ".js";
		writeResourceFile(fileName, _directory, fileName, generatedString, _element);
	}

	public String generateDao(DomainObjectModel _domainObjectModel)
	{
		String domainName = _domainObjectModel.getClassName();
		SqlGenerator databaseGenerator = new SqlGenerator();

		StringTemplate daoStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/Dao");
		StringTemplate primaryKeyMethodsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/PrimaryKeyMethods");
		StringTemplate updateMethodStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/UpdateMethod");
		StringTemplate updateMethodNotSupportedStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/UpdateMethodNotSupported");

		java.util.Iterator attributes = _domainObjectModel.getAttributes();

		boolean hasPrimaryKey = false;
		boolean updateIsAvailable = false;

		String isUndoable = "false";

		while (attributes.hasNext() == true)
		{
			String isSearchable = "false";

			Attribute attribute = (Attribute) attributes.next();
			Annotation persist = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Persistable");
			Annotation search = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Searchable");
			Annotation undo = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Undoable");

			isUndoable = (undo != null) ? findValue(undo, "isUndoable()") : "false";

			String requestPersistence = findValue(persist, "isPersistable()");
			String requestSearchable = findValue(persist, "isSearchable()");			

			if ("true".equals(requestPersistence) == true)
			{
				String type = attribute.getType();

				if (isPersistable(type) == false)
				{
					throw new RuntimeException("No persistence support for complex object types like: " + type);
				}

				String fieldName = attribute.getName();
				String name = attribute.getName();
				String isPrimaryKey = findValue(persist, "isPrimaryKey()");
				String rangeable = findValue(persist, "rangeable()");
				String isImmutable = findValue(persist, "immutable()");

				if (search != null)
				{
					isSearchable = findValue(search, "isSearchable()");
				}

				hasPrimaryKey = true;

				StringTemplate daoAttributeStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/DaoAttribute");

				daoAttributeStringTemplate.setAttribute("findByThisAttributeSql", databaseGenerator.generateFindByThisAttributeSql(_domainObjectModel, fieldName));
				daoAttributeStringTemplate.setAttribute("attributeName", name);
				daoAttributeStringTemplate.setAttribute("attributeType", type);
				daoAttributeStringTemplate.setAttribute("uppercaseAttributeName", upperTheFirstCharacter(name));
				daoAttributeStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
				daoStringTemplate.setAttribute("collectByAttribute", daoAttributeStringTemplate.toString());

				StringTemplate daoRangeStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/DaoRange");

				daoRangeStringTemplate.setAttribute("findByRangeSql", databaseGenerator.generateFindThisRangeSql(_domainObjectModel, fieldName));
				daoRangeStringTemplate.setAttribute("rangeType", type);
				daoRangeStringTemplate.setAttribute("uppercaseAttribute", upperTheFirstCharacter(name));
				daoRangeStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
				daoStringTemplate.setAttribute("collectByRange", daoRangeStringTemplate.toString());
			}
		}

		if (_domainObjectModel.isClockable() == true)
		{
			updateIsAvailable = true;
		}

		if (hasPrimaryKey == true)
		{
			updateMethodStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
			updateMethodStringTemplate.setAttribute("lowerCaseClassName", lowerTheFirstCharacter(_domainObjectModel.getClassName()));
			primaryKeyMethodsStringTemplate.setAttribute("updateMethod", updateMethodStringTemplate.toString());

			primaryKeyMethodsStringTemplate.setAttribute("fullClassName", _domainObjectModel.getPackageName() + "." + _domainObjectModel.getClassName());
			primaryKeyMethodsStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
			primaryKeyMethodsStringTemplate.setAttribute("lowerCaseClassName", lowerTheFirstCharacter(_domainObjectModel.getClassName()));

			daoStringTemplate.setAttribute("primaryKeyMethods", primaryKeyMethodsStringTemplate.toString());
		}

		daoStringTemplate.setAttribute("generatorComment", comment);
		daoStringTemplate.setAttribute("upperCaseFirstLetterDomainClassName", upperTheFirstCharacter(domainName));
		daoStringTemplate.setAttribute("package", _domainObjectModel.getRootPackageName());
		daoStringTemplate.setAttribute("fullClassName", _domainObjectModel.getPackageName() + "." + _domainObjectModel.getClassName());
		daoStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
		daoStringTemplate.setAttribute("lowerCaseClassName", lowerTheFirstCharacter(_domainObjectModel.getClassName()));
		daoStringTemplate.setAttribute("selectAllSql", databaseGenerator.generateSelectSql(_domainObjectModel));

		return daoStringTemplate.toString();
	}

	public String generateActiveRecord(DomainObjectModel _domainObjectModel)
	{
		String domainName = _domainObjectModel.getClassName();
		StringTemplate activeRecordStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ActiveRecord");

		activeRecordStringTemplate.setAttribute("package", _domainObjectModel.getRootPackageName());
		activeRecordStringTemplate.setAttribute("appName", _domainObjectModel.getAppName());
		activeRecordStringTemplate.setAttribute("domainName", lowerTheFirstCharacter(domainName));
		activeRecordStringTemplate.setAttribute("upperCaseFirstLetterDomainClassName", upperTheFirstCharacter(domainName));
		activeRecordStringTemplate.setAttribute("fullyQualifiedDomainClassName", _domainObjectModel.getFullyQualifiedClassName());
		activeRecordStringTemplate.setAttribute("allLowerCaseClassName", _domainObjectModel.getClassName().toLowerCase()); 

		boolean processedDatable = false;
		
		java.util.Iterator attributes = _domainObjectModel.getAttributes();

		while (attributes.hasNext() == true)
		{
			Attribute attribute = (Attribute) attributes.next();
			String name = attribute.getName();

			Annotation datable = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Datable");
			Annotation persist = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Persistable");
			
			String requestDatable = findValue(datable, "isDatable()");
			String isForeignKey = findValue(persist, "mapTo()");
			
			if (processedDatable == false)
			{
				String dateTimePatterns = ("true".equals(requestDatable) == true) ? findValue(datable, "dateTimePatterns()") : "{ \"MM-dd-yyyy\", \"yyyy-MM-dd\", \"MM/dd/yyyy\", \"yyyy/MM/dd\", \"yyyy-MM-dd HH:mm:ss\", \"MM-dd-yyyy HH:mm:ss\", \"MM/dd/yyyy HH:mm:ss\", \"yyyy/MM/dd HH:mm:ss\", \"MM-dd-yyyy\", \"yyyy-MM-dd\", \"MM/dd/yyyy\", \"yyyy/MM/dd\"}";
				activeRecordStringTemplate.setAttribute("dateTimePatterns", dateTimePatterns.replaceAll("^\\s*\\{", "[").replaceAll("\\}$\\s*", "]"));
			}

			processedDatable = true;

			if ("false".equals(isForeignKey) == false)
			{
				activeRecordStringTemplate.setAttribute("addNameToForeignKeySet", "this.foreignKeySet.add(\"" + name + "\");");
			}

			activeRecordStringTemplate.setAttribute("defineProperty", "activeRecord." + name + " = null;");
			activeRecordStringTemplate.setAttribute("setMethod", "activeRecord.set" + upperTheFirstCharacter(name) + " = function(_" + name + ") { this." + name + " = _" + name + "; }");
			activeRecordStringTemplate.setAttribute("getMethod", "activeRecord.get" + upperTheFirstCharacter(name) + " = function() { return this." + name + "; }");

			activeRecordStringTemplate.setAttribute("collectByAttribute", "activeRecord.collectBy" + upperTheFirstCharacter(name) + " = function(_name, _offset, _limit, _orderBy) { return this.convertToActiveRecordArray(this.dao.collectBy" + upperTheFirstCharacter(name) + "(_name, _offset, _limit, _orderBy)); }");
			activeRecordStringTemplate.setAttribute("collectByRange", "activeRecord.collectBy" + upperTheFirstCharacter(name) + "Range = function(_begin, _end, _offset, _limit, _orderBy) { return this.convertToActiveRecordArray(this.dao.collectBy" + upperTheFirstCharacter(name) + "(_begin, _end, _offset, _limit, _orderBy)); }");

			activeRecordStringTemplate.setAttribute("addPropertyName", "propertyList.push(airlift.string(\"" + name + "\"));");
			activeRecordStringTemplate.setAttribute("validateAttribute", "errorArray.concat(" + upperTheFirstCharacter(domainName) + "Validator.validate" + upperTheFirstCharacter(name) + "(this." + name + "));");
		}

		return activeRecordStringTemplate.toString();
	}

	public String generateValidationObject(DomainObjectModel _domainObjectModel)
	{
		StringTemplate validationObjectStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationObject");

		java.util.Iterator attributes = _domainObjectModel.getAttributes();

		while (attributes.hasNext() == true)
		{
			Attribute attribute = (Attribute) attributes.next();

			String name = attribute.getName();
			String type = attribute.getType();

			Annotation persist = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Persistable");
			String requestPersistence = findValue(persist, "isPersistable()");

			Annotation present = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Presentable");
			String requestPresentable = findValue(present, "isPresentable()");

			Annotation datable = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Datable");
			String requestDatable = findValue(datable, "isDatable()");

			String label = findValue(present, "label()");
			label = (label != null && StringUtils.isWhitespace(label) == false) ? label : name;
			
			String format = ".*";
			String dateMask = "MM-dd-yyyy";
			
			if ("true".equalsIgnoreCase(requestPresentable) == true)
			{
				format = findValue(present, "hasFormat()");
			}

			if ("true".equalsIgnoreCase(requestDatable) == true)
			{
				String dateMaskPatterns = findValue(datable, "dateTimePatterns()");

				String[] dateMaskPatternsArray = dateMaskPatterns.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\"", "").trim().split(",");;
				
				if (dateMaskPatternsArray != null && dateMaskPatternsArray.length > 0)
				{
					dateMask = dateMaskPatternsArray[0];
				}
			}

			int length = Integer.parseInt(findValue(persist, "maxLength()"));
			int precision = Integer.parseInt(findValue(persist, "precision()"));
			int scale = Integer.parseInt(findValue(persist, "scale()"));
			boolean nullable = Boolean.parseBoolean(findValue(persist, "nullable()"));
			
			String semanticType = findValue(persist, "semanticType()");
			String minimum = findValue(persist, "minimumValue()");
			String maximum = findValue(persist, "maximumValue()");
			String isPrimaryKey = findValue(persist, "isPrimaryKey()");
			String isForeignKey = findValue(persist, "mapTo()");
			
			String validationFunctionName = "noop";
			String jsonParams = "{}";

			StringTemplate validationFunctionStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunction");

			validationFunctionStringTemplate.setAttribute("className", _domainObjectModel.getClassName());
			validationFunctionStringTemplate.setAttribute("propertyName", upperTheFirstCharacter(name));
			
			if (nullable == false || "true".equalsIgnoreCase(isPrimaryKey) == true || "false".equalsIgnoreCase(isForeignKey) == false)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "isRequired");
				validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"failureMessage\"] = \"This is a required field.\";");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);

				validationFunctionStringTemplate.setAttribute("requiredValidationFunction", validationFunctionInnardsStringTemplate.toString());
			}

			if (isNumericType(type) == true)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "isNumeric");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);

				if ("java.lang.Long".equals(type) == true || "long".equals(type) == true ||
				      "java.lang.Short".equals(type) == true || "short".equals(type) == true ||
				      "java.lang.Integer".equals(type) == true || "int".equals(type) == true ||
				      "java.math.BigInteger".equals(type) == true)
				{
				    validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"onlyInteger\"] = true;");
				}

				if ("".equals(maximum) != true)
				{
				    validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"maximum\"] = " + maximum + ";");
				}

				if ("".equals(minimum) != true)
				{
				    validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"minimum\"] = " + minimum + ";");
				}

				validationFunctionStringTemplate.setAttribute("validationFunctionInnards", validationFunctionInnardsStringTemplate.toString());
			}
			else if ("java.lang.String".equals(type) == true || "java.lang.Character".equals(type) == true || "char".equals(type) == true)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "hasLength");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);
				validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"maximum\"] = " + length + ";");

				validationFunctionStringTemplate.setAttribute("validationFunctionInnards", validationFunctionInnardsStringTemplate.toString());
			}

			//TODO  What is the impact of not validating the time
			//component of a java.util.Date.  When do people use
			//java.util.Date in web applications in the context that
			//would require validation. In other words when would an
			//application accept a java.util.Date through a form?
			else if ("java.util.Date".equals(type) == true || "java.sql.Date".equals(type) == true)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "isDate");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);
				validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"mask\"] = \"" + dateMask + "\";");

				validationFunctionStringTemplate.setAttribute("validationFunctionInnards", validationFunctionInnardsStringTemplate.toString());
			}

			
			if (format != null && "\".*\"".equals(format) == false)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "hasFormat");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);
				validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"pattern\"] = new RegExp(\"" + format + "\",\"i\");");

				validationFunctionStringTemplate.setAttribute("validationFunctionInnards", validationFunctionInnardsStringTemplate.toString());
			}

			if ("airlift.generator.Persistable.Semantic.EMAIL".equalsIgnoreCase(semanticType) == true)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "isEmail");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);
				validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"failureMessage\"] = \"" + label + " is not a valid email address.\";");

				validationFunctionStringTemplate.setAttribute("validationFunctionInnards", validationFunctionInnardsStringTemplate.toString());
			}
			else if ("airlift.generator.Persistable.Semantic.ZIPCODE".equalsIgnoreCase(semanticType) == true)
			{
				StringTemplate validationFunctionInnardsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ValidationFunctionInnards");

				validationFunctionInnardsStringTemplate.setAttribute("validationFunctionName", "isZipCode");
				validationFunctionInnardsStringTemplate.setAttribute("propertyName", name);
				validationFunctionInnardsStringTemplate.setAttribute("validationParameters", "parameters[\"failureMessage\"] = \"" + label + " is not a valid zip code.\";");

				validationFunctionStringTemplate.setAttribute("validationFunctionInnards", validationFunctionInnardsStringTemplate.toString());
			}

			validationObjectStringTemplate.setAttribute("validationFunction", validationFunctionStringTemplate.toString());
		}
		
		validationObjectStringTemplate.setAttribute("className", _domainObjectModel.getClassName());
		validationObjectStringTemplate.setAttribute("generatorComment", comment);

		return validationObjectStringTemplate.toString();
	}
}