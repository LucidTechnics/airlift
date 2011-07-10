/*
 Copyright 2011, Lucid Technics, LLC.

 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 except in compliance with the License. You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in
 writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 specific language governing permissions and limitations under the License.
*/

package airlift.generator;

import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.lang.StringUtils;

import javax.lang.model.element.Element;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class JavaScriptGenerator.
 */
public class JavaScriptGenerator
   extends Generator
{
	
	/** The comment. */
	public String comment = "This JavaScript code has been generated by airlift. Do not modify this code.";

	/* (non-Javadoc)
	 * @see airlift.generator.Generator#generate(java.lang.String, java.lang.String, javax.lang.model.element.Element, airlift.generator.DomainObjectModel, java.util.Map)
	 */
	public void generate(String _appName,
				String _directory,
				Element _element,
				DomainObjectModel _domainObjectModel,
				Map<String, DomainObjectModel> _elementNameToDomainObjectModelMap)
	{
		String generatedString = generateValidationObject(_domainObjectModel);
		String fileName =  _appName + "/airlift/validation/domain/" + _domainObjectModel.getClassName() + ".js";
		writeResourceFile(fileName, _directory, generatedString, _element);

		generatedString = generateDao(_domainObjectModel);
		fileName =  _appName + "/airlift/dao/" + _domainObjectModel.getClassName() + ".js";
		writeResourceFile(fileName, _directory, generatedString, _element);

		generatedString = generateActiveRecord(_domainObjectModel);
		fileName =  _appName + "/airlift/activerecord/" + _domainObjectModel.getClassName() + ".js";
		writeResourceFile(fileName, _directory, generatedString, _element);
	}
	
	/**
	 * Generate dao.
	 *
	 * @param _domainObjectModel the _domain object model
	 * @return the string
	 */
	public String generateDao(DomainObjectModel _domainObjectModel)
	{
		String domainName = _domainObjectModel.getClassName();
		SqlGenerator databaseGenerator = new SqlGenerator();

		StringTemplate daoStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/Dao");
		StringTemplate primaryKeyMethodsStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/PrimaryKeyMethods");
		StringTemplate updateMethodStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/UpdateMethod");
		StringTemplate updateMethodNotSupportedStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/UpdateMethodNotSupported");

		//Encryption templates ...
		StringTemplate encryptionSetupTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/EncryptionSetup");
		StringTemplate encryptInvokationStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/EncryptInvokation");
		StringTemplate setDataObjectEncryptedFieldStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/SetDataObjectEncryptedField");
		StringTemplate decryptInvokationStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/DecryptInvokation");

		java.util.Iterator attributes = _domainObjectModel.getAttributes();

		boolean hasPrimaryKey = false;
		boolean updateIsAvailable = false;

		String isUndoable = "false";
		boolean processedEncryptionHeader = false;
		boolean thisDomainIsSearchable = false;
		
		while (attributes.hasNext() == true)
		{
			Attribute attribute = (Attribute) attributes.next();
			Annotation persist = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Persistable");
			Annotation undo = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Undoable");

			String encrypted = findValue(persist, "encrypted()");

			isUndoable = (undo != null) ? findValue(undo, "isUndoable()") : "false";

			String requestPersistence = findValue(persist, "isPersistable()");

			String isSearchable = "false";
			String isIndexable = "false";

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
				String isForeignKey = findValue(persist, "isForeignKey()");
				String rangeable = findValue(persist, "rangeable()");
				String isImmutable = findValue(persist, "immutable()");

				isIndexable = findValue(persist, "isIndexable()");
				isSearchable = findValue(persist, "isSearchable()");

				if ("true".equalsIgnoreCase(isSearchable) == true)
				{
					thisDomainIsSearchable = true;

					String indexAddAll = "";

					if ("java.util.Date".equals(type) == true)
					{
						indexAddAll = "indexSet.addAll(airlift.tokenizeIntoDateParts(_activeRecord." + name + ", \"" + name + "\"));"; 
					}
					else
					//For all other types change to a string and index it!
					{
						indexAddAll = "indexSet.addAll(airlift.tokenizeIntoNGrams(_activeRecord." + name + "));"; 
					}

					daoStringTemplate.setAttribute("indexAddAll", indexAddAll);
				}

				if ("true".equalsIgnoreCase(isForeignKey) == true)
				{
					daoStringTemplate.setAttribute("indexAddAll", "indexSet.add(_activeRecord." + name + ");"); 
				}
				
				hasPrimaryKey = true;

				if ("true".equalsIgnoreCase(isIndexable) == true || "true".equalsIgnoreCase(isForeignKey) == true)
				{

					if (type.endsWith("[]") == true ||
						  type.startsWith("java.util.List") == true ||
						  type.startsWith("java.util.Set") == true ||
						  type.startsWith("java.util.ArrayList") == true ||
						  type.startsWith("java.util.HashSet") == true)

					{
						StringTemplate daoMembershipStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/DaoIntersection");

						daoMembershipStringTemplate.setAttribute("uppercaseAttributeName", upperTheFirstCharacter(name));
						daoMembershipStringTemplate.setAttribute("attribute", name);
						daoMembershipStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));

						daoStringTemplate.setAttribute("collectByIntersection", daoMembershipStringTemplate.toString());
					}

					StringTemplate daoMembershipStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/DaoMembership");

					daoMembershipStringTemplate.setAttribute("uppercaseAttributeName", upperTheFirstCharacter(name));
					daoMembershipStringTemplate.setAttribute("attribute", name);
					daoMembershipStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));

					daoStringTemplate.setAttribute("collectByMembership", daoMembershipStringTemplate.toString());

					StringTemplate daoAttributeStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/dao/DaoAttribute");

					daoAttributeStringTemplate.setAttribute("uppercaseAttributeName", upperTheFirstCharacter(name));
					daoAttributeStringTemplate.setAttribute("attribute", name);
					daoAttributeStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));

					daoStringTemplate.setAttribute("collectByAttribute", daoAttributeStringTemplate.toString());	
				}

				if ("id".equalsIgnoreCase(name) == false)
				{
					if (type.startsWith("java.util.List") == true)
					{
						daoStringTemplate.setAttribute("copyFromEntityToActiveRecord", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _activeRecord." + name + " = (_entity.getProperty(\"" + name + "\") && (_entity.getProperty(\"" + name + "\") instanceof Packages.java.util.Collection) && airlift.l(_entity.getProperty(\"" + name + "\")))||(airlift.l()); }");
					}
					else if (type.startsWith("java.util.Set") == true)
					{
						daoStringTemplate.setAttribute("copyFromEntityToActiveRecord", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _activeRecord." + name + " = (_entity.getProperty(\"" + name + "\") &&  (_entity.getProperty(\"" + name + "\") instanceof Packages.java.util.Collection) && airlift.s(_entity.getProperty(\"" + name + "\")))||(airlift.s()); }");
					}
					else
					{
						daoStringTemplate.setAttribute("copyFromEntityToActiveRecord", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _activeRecord." + name + " = (_entity.getProperty(\"" + name + "\") && converter.convert( _entity.getProperty(\"" + name + "\"), airlift.cc(\"" + type + "\")))||null; }");
					}

					if ("true".equalsIgnoreCase(isIndexable) == true || "true".equalsIgnoreCase(isForeignKey) == true )
					{
						if (type.startsWith("java.util.List") == true)
						{
							daoStringTemplate.setAttribute("copyFromActiveRecordToEntity", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _entity.setProperty(\"" + name + "\", new Packages.java.util.ArrayList(_activeRecord." + name + ")); }");
						}
						else if (type.startsWith("java.util.Set") == true)
						{
							daoStringTemplate.setAttribute("copyFromActiveRecordToEntity", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _entity.setProperty(\"" + name + "\", new Packages.java.util.HashSet(_activeRecord." + name + ")); }");
						}
						else
						{
							daoStringTemplate.setAttribute("copyFromActiveRecordToEntity", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _entity.setProperty(\"" + name + "\", _activeRecord." + name + "); }");
						}
					}
					else
					{
						if (type.startsWith("java.util.List") == true)
						{
							daoStringTemplate.setAttribute("copyFromActiveRecordToEntity", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _entity.setUnindexedProperty(\"" + name + "\", new Packages.java.util.ArrayList(_activeRecord." + name + ")); }");
						}
						else if (type.startsWith("java.util.Set") == true)
						{
							daoStringTemplate.setAttribute("copyFromActiveRecordToEntity", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _entity.setUnindexedProperty(\"" + name + "\", new Packages.java.util.HashSet(_activeRecord." + name + ")); }");
						}
						else
						{
							daoStringTemplate.setAttribute("copyFromActiveRecordToEntity", "if (airlift.filterContains(filter, \"" + name + "\") === contains) { _entity.setUnindexedProperty(\"" + name + "\", _activeRecord." + name + "); }");
						}
					}
				}
				else
				{
					daoStringTemplate.setAttribute("copyFromEntityToActiveRecord", "if (airlift.filterContains(filter, \"id\") === contains) { _activeRecord.id = _entity.getKey().getName(); }");
				}

				if ("true".equalsIgnoreCase(encrypted) == true)
				{
					if (processedEncryptionHeader == false)
					{
						daoStringTemplate.setAttribute("encryptionSetup", encryptionSetupTemplate.toString());
						processedEncryptionHeader = true;
					}

					String encryptedName = name + "Encrypted";

					String encryptionConversionFunction = determineEncryptionConversionFunction(type);
					String decryptionConversionFunction = determineDecryptionConversionFunction(type);

					encryptInvokationStringTemplate.setAttribute("name", name);
					encryptInvokationStringTemplate.setAttribute("encryptedName", encryptedName);
					encryptInvokationStringTemplate.setAttribute("conversionFunction", encryptionConversionFunction);

					setDataObjectEncryptedFieldStringTemplate.setAttribute("encryptedName", encryptedName);

					decryptInvokationStringTemplate.setAttribute("name", name);
					decryptInvokationStringTemplate.setAttribute("encryptedName", encryptedName);
					decryptInvokationStringTemplate.setAttribute("conversionFunction", decryptionConversionFunction);
				}

			}
		}

		if (_domainObjectModel.isClockable() == true)
		{
			updateIsAvailable = true;
		}

		if (hasPrimaryKey == true)
		{
			updateMethodStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
			updateMethodStringTemplate.setAttribute("package", _domainObjectModel.getRootPackageName());

			if (thisDomainIsSearchable == true)
			{
				updateMethodStringTemplate.setAttribute("index", "var indexList = this.index(_activeRecord), index = new Packages.com.google.appengine.api.datastore.Entity(\"" + _domainObjectModel.getClassName() + "Index\", _activeRecord.id, parentKey); index.setProperty(\"index\", indexList);");
				updateMethodStringTemplate.setAttribute("writeIndex", "var indexWritten = parentWritten && dao.multiTry(function() { datastore.put(transaction, index);  return true; }, 5, \"Encountered this error while accessing the datastore for " + _domainObjectModel.getClassName() + " index update\", function() { transaction.rollbackAsync(); });");
				updateMethodStringTemplate.setAttribute("indexWritten", "indexWritten && ");
			}
			else
			{
				//This makes the dao javascript file look better. :)
				updateMethodStringTemplate.setAttribute("indexWritten", "");
			}

			primaryKeyMethodsStringTemplate.setAttribute("updateMethod", updateMethodStringTemplate.toString());
			primaryKeyMethodsStringTemplate.setAttribute("package", _domainObjectModel.getRootPackageName());
			primaryKeyMethodsStringTemplate.setAttribute("fullClassName", _domainObjectModel.getPackageName() + "." + _domainObjectModel.getClassName());
			primaryKeyMethodsStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));

			daoStringTemplate.setAttribute("primaryKeyMethods", primaryKeyMethodsStringTemplate.toString());
		}

		if (processedEncryptionHeader == true)
		{
			daoStringTemplate.setAttribute("setDataObjectEncryptedField", setDataObjectEncryptedFieldStringTemplate.toString());
			daoStringTemplate.setAttribute("encryptedAttribute", encryptInvokationStringTemplate.toString());
			daoStringTemplate.setAttribute("decryptToActiveRecordAttribute", decryptInvokationStringTemplate.toString());
		}

		if (thisDomainIsSearchable == true)
		{
			daoStringTemplate.setAttribute("index", "var indexList = this.index(_activeRecord), index = new Packages.com.google.appengine.api.datastore.Entity(\"" + _domainObjectModel.getClassName() + "Index\", id, parentKey); index.setProperty(\"index\", indexList);");
			daoStringTemplate.setAttribute("writeIndex", "var indexWritten = parentWritten && dao.multiTry(function() { datastore.put(transaction, index);  return true; }, 5, \"Encountered this error while accessing the datastore for " + _domainObjectModel.getClassName() + " index insert\", function() { transaction.rollbackAsync(); });");
			daoStringTemplate.setAttribute("indexWritten", "indexWritten && ");
		}
		else
		{
			//This makes the dao javascript file look better. :)
			updateMethodStringTemplate.setAttribute("indexWritten", "");
		}
		
		daoStringTemplate.setAttribute("generatorComment", comment);
		daoStringTemplate.setAttribute("upperCaseFirstLetterDomainClassName", upperTheFirstCharacter(domainName));
		daoStringTemplate.setAttribute("package", _domainObjectModel.getRootPackageName());
		daoStringTemplate.setAttribute("fullClassName", _domainObjectModel.getPackageName() + "." + _domainObjectModel.getClassName());
		daoStringTemplate.setAttribute("className", upperTheFirstCharacter(_domainObjectModel.getClassName()));
		daoStringTemplate.setAttribute("lowerCaseClassName", _domainObjectModel.getClassName().toLowerCase());
		daoStringTemplate.setAttribute("selectAllSql", databaseGenerator.generateSelectSql(_domainObjectModel));
		daoStringTemplate.setAttribute("findKeysSql", databaseGenerator.generateFindKeysSql(_domainObjectModel));
		
		return daoStringTemplate.toString();
	}

	/**
	 * Generate active record.
	 *
	 * @param _domainObjectModel the _domain object model
	 * @return the string
	 */
	public String generateActiveRecord(DomainObjectModel _domainObjectModel)
	{
		String domainName = _domainObjectModel.getClassName();
		StringTemplate activeRecordStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/ActiveRecord");
		StringTemplate stringBufferStringTemplate = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/AttributeStringBufferAppends");
		
		activeRecordStringTemplate.setAttribute("package", _domainObjectModel.getRootPackageName());
		activeRecordStringTemplate.setAttribute("appName", _domainObjectModel.getAppName());
		activeRecordStringTemplate.setAttribute("upperCaseFirstLetterDomainClassName", upperTheFirstCharacter(domainName));
		activeRecordStringTemplate.setAttribute("fullyQualifiedDomainClassName", _domainObjectModel.getFullyQualifiedClassName());
		activeRecordStringTemplate.setAttribute("allLowerCaseClassName", _domainObjectModel.getClassName().toLowerCase());
		activeRecordStringTemplate.setAttribute("className", _domainObjectModel.getClassName());
		
		boolean processedDatable = false;
		
		java.util.Iterator attributes = _domainObjectModel.getAttributes();

		//table template parts
		activeRecordStringTemplate.setAttribute("tableTemplate", "<table id=\"$id$\" class=\"$class$\"><thead><tr> $th, tha: {t,a | <th $a$> $t$ </th>}$ </tr></thead><tbody>$tr; separator=\"\\n\"$</tbody></table>");
		activeRecordStringTemplate.setAttribute("trTemplate", "<tr $trAttribute$> $td, tda: {t,a | <td $a$> $t$ </td>}; separator=\"\\n\"$ </tr>");
		activeRecordStringTemplate.setAttribute("anchorTemplate", "<a id=\"$id$\" class=\"$class; separator=\" \"$\" href=\"$href$\" hreflang=\"$hreflang$\" rel=\"$rel$\" rev=\"$rev$\" target=\"$target$\">$label$</a>");

		//form template parts
		activeRecordStringTemplate.setAttribute("formTemplate", "<form id=\"$formId$\" action=\"$formName$\" method=\"post\">$fieldSet:{f |$f$ $\\n$}$<fieldset id=\"$fieldSetId$\" class=\"submit\"><input id =\"$buttonId$\" value=\"$buttonName$\" class=\"submit\" type=\"submit\" onclick=\"$onclick$\" /></fieldset></form>");
		activeRecordStringTemplate.setAttribute("fieldSetTemplate", "<fieldset id=\"$fieldSetId$\" class=\"$domainName$\"><legend><span>$groupName$</span></legend><ol>$formEntry:{f |$f$ $\n$}$</ol>$hiddenFormEntry:{f |$f$ $\n$}$</fieldset>");
		activeRecordStringTemplate.setAttribute("formEntryTemplate", "<li id=\"$count$\"><label for=\"$name$\"> $label$  <em id=\"$emId$\" class=\"$emClass; separator=\" \"$\"> $message$</em></label> $input$ </li>");
		activeRecordStringTemplate.setAttribute("hiddenFormEntryTemplate", "<input  type=\"hidden\" name=\"$name$\" value=\"$value$\" id=\"$id$\" />");
		activeRecordStringTemplate.setAttribute("inputTemplate", "<input type=\"$type$\" name=\"$name$\" value=\"$value$\" maxlength=\"$maxLength$\" size=\"$displayLength$\" id=\"$id$\" class=\"$inputClass$\" />");
		activeRecordStringTemplate.setAttribute("readOnlyInputTemplate", "<input type=\"$type$\" name=\"$name$\" value=\"$value$\" maxlength=\"$maxLength$\" size=\"$displayLength$\" id=\"$id$\" readonly=\"readonly\" class=\"$inputClass$\" />");
		
		while (attributes.hasNext() == true)
		{
			int count = 0;
			Attribute attribute = (Attribute) attributes.next();
			String name = attribute.getName();
			String type = attribute.getType();
			String getterName = getGetterName(name);
			String setterName = getSetterName(name);

			Annotation datable = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Datable");
			Annotation persist = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Persistable");
			Annotation present = (Annotation) _domainObjectModel.getAnnotation(attribute,"airlift.generator.Presentable");

			String label = findValue(present, "label()");
			String dateMask = findValue(present, "dateTimePattern()"); 
			boolean nullable = Boolean.parseBoolean(findValue(persist, "nullable()"));
			String fieldSetName = findValue(persist, "fieldSetName()");
						
			String requestDatable = findValue(datable, "isDatable()");
			String isForeignKey = findValue(persist, "mapTo()");

			activeRecordStringTemplate.setAttribute("populateFormTemplate", "if ((airlift.filterContains(orderedPropertyList, \"" + name + "\") === true) || (airlift.filterContains(filter, \"" + name + "\") === contains)) { airlift.populateFormTemplate(formTemplate, groupName, \"" + name + "\", this); }");
			
			String isIndexable = "false";
			isIndexable = findValue(persist, "isIndexable()");

			if (processedDatable == false)
			{
				String dateTimePatterns = ("true".equals(requestDatable) == true) ? findValue(datable, "dateTimePatterns()") : "{ \"MM-dd-yyyy\", \"MM-dd-yyyy HH:mm:ss\", \"MM-dd-yyyy Z\", \"MM-dd-yyyy HH:mm:ss Z\", \"MMM d, yyyy h:mm:ss a\"}";
				activeRecordStringTemplate.setAttribute("dateTimePatterns", dateTimePatterns.replaceAll("^\\s*\\{", "[").replaceAll("\\}$\\s*", "]"));
			}

			processedDatable = true;

			if ("false".equalsIgnoreCase(isForeignKey) == false)
			{
				activeRecordStringTemplate.setAttribute("addNameToForeignKeySet", "activeRecord.foreignKeySet.add(\"" + name + "\");");
				activeRecordStringTemplate.setAttribute("addForeignKeyName", "foreignKeyList.push(airlift.string(\"" + name + "\"));");
				activeRecordStringTemplate.setAttribute("restifyForeignKey", "this[\"" + name + "\"] && impl.set" + upperTheFirstCharacter(name) + "(base + \"a/" + name.toLowerCase().replaceAll("id", "") + "/\" + this[\"" + name + "\"]);");
				activeRecordStringTemplate.setAttribute("foreignKeyListEntry", "\"" + name + "\"");

				activeRecordStringTemplate.setAttribute("assignForeignKeyFromRestContext", "this." + name + " = ((airlift.isDefined(this." + name + ") === false)  && _restContext.getIdValue(\"" + name.toLowerCase().replaceAll("id", "") + ".id\"))||this." + name + ";");
				activeRecordStringTemplate.setAttribute("validateForeignKey", "errorList.concat(this.validator.validate" + upperTheFirstCharacter(name) + "(((this." + name + " && Packages.airlift.util.FormatUtil.format(this." + name + "))||\"\") + \"\"));");
			}

			
			activeRecordStringTemplate.setAttribute("defineProperty", "activeRecord." + name + " = null;");
			activeRecordStringTemplate.setAttribute("setMethod", "activeRecord.set" + upperTheFirstCharacter(name) + " = function(_" + name + ") { this." + name + " = _" + name + "; return this; };");
			activeRecordStringTemplate.setAttribute("getMethod", "activeRecord.get" + upperTheFirstCharacter(name) + " = function() { return this." + name + "; };");

			activeRecordStringTemplate.setAttribute("copyToActiveRecord", "if (airlift.isDefined(this." + name + ") === true && (airlift.filterContains(filter, \"" + name + "\") === contains)) { _activeRecord[\"" + name + "\"] = this[\"" + name + "\"]; }");
			activeRecordStringTemplate.setAttribute("copyFromActiveRecord", "if (airlift.isDefined(_activeRecord." + name + ") === true && (airlift.filterContains(filter, \"" + name + "\") === contains)) { this[\"" + name + "\"] = _activeRecord[\"" + name + "\"]; }");
			
			if ("true".equalsIgnoreCase(isIndexable) == true || "true".equalsIgnoreCase(isForeignKey) == true)
			{
				if (type.endsWith("[]") == true ||
					  type.startsWith("java.util.List") == true ||
					  type.startsWith("java.util.Set") == true ||
					  type.startsWith("java.util.ArrayList") == true ||
					  type.startsWith("java.util.HashSet") == true)

				{
					activeRecordStringTemplate.setAttribute("collectByIntersection", "activeRecord.collectBy" + upperTheFirstCharacter(name) + "Intersection = function(_value, _config) { if (_config && _config.checkSecurity) { airlift.checkAllowed(this.retrieveDomainName(), \"GET\", true); } return this.dao.collectBy" + upperTheFirstCharacter(name) + "Intersection(_value, _config||{}); };");
				}

				activeRecordStringTemplate.setAttribute("collectByMembership", "activeRecord.collectBy" + upperTheFirstCharacter(name) + "Membership = function(_value, _config) { if (_config && _config.checkSecurity) { airlift.checkAllowed(this.retrieveDomainName(), \"GET\", true); } return this.dao.collectBy" + upperTheFirstCharacter(name) + "Membership(_value, _config||{}); };");
				activeRecordStringTemplate.setAttribute("collectByAttribute", "activeRecord.collectBy" + upperTheFirstCharacter(name) + " = function(_value, _config) { if (_config && _config.checkSecurity) { airlift.checkAllowed(this.retrieveDomainName(), \"GET\", true); } return this.dao.collectBy" + upperTheFirstCharacter(name) + "(_value, _config||{}); };");
			}

			activeRecordStringTemplate.setAttribute("addPropertyName", "propertyList.push(airlift.string(\"" + name + "\"));");
			activeRecordStringTemplate.setAttribute("copyPropertyToImpl", "this." + name + " && _impl.set" + upperTheFirstCharacter(name) + "(this." + name + ");");
			activeRecordStringTemplate.setAttribute("propertyListEntry", "\"" + name + "\"");
			
			if ("id".equalsIgnoreCase(name) == false)
			{
				if (type.startsWith("java.util.List") == true)  
				{
					activeRecordStringTemplate.setAttribute("copyPropertyFromRequestMap", "value = _attributeMap.get(\"" + name + "\")||null; this.copyValueArrayToCollection(value, new Packages.java.util.ArrayList());");
				}
				else if (type.startsWith("java.util.Set") == true)
				{
					activeRecordStringTemplate.setAttribute("copyPropertyFromRequestMap", "value = _attributeMap.get(\"" + name + "\")||null; this.copyValueArrayToCollection(value, new Packages.java.util.HashSet());");
				}
				else
				{
					if (type.equalsIgnoreCase("java.lang.String") == true ||
						  type.equalsIgnoreCase("java.lang.Character") == true)
					{
						activeRecordStringTemplate.setAttribute("copyPropertyFromRequestMap",  "value = (_attributeMap.get(\"" + name + "\") && _attributeMap.get(\"" + name + "\")[0])||null; try { this." + name + " =  (value && Packages.airlift.util.FormatUtil.format(converter.convert(value, airlift.cc(\"" + type + "\"))))||null; } catch(e) { this.addError(\"" + name + "\", e.javaException.getMessage(), \"conversion\"); }");
					}
					else
					{
						activeRecordStringTemplate.setAttribute("copyPropertyFromRequestMap",  "value = (_attributeMap.get(\"" + name + "\") && _attributeMap.get(\"" + name + "\")[0] && (airlift.isWhitespace(_attributeMap.get(\"" + name + "\")[0]) === false) && _attributeMap.get(\"" + name + "\")[0])||null; try { this." + name + " =  (value && converter.convert(value, airlift.cc(\"" + type + "\")))||null; } catch(e) { this.addError(\"" + name + "\", e.javaException.getMessage(), \"conversion\"); }");
					}

					if ("false".equals(isForeignKey) == true)
					{
						if (type.equalsIgnoreCase("java.util.Date") == true ||
							  type.equalsIgnoreCase("java.sql.Date") == true ||
							  type.equalsIgnoreCase("java.sql.Timestamp") == true)
						{
							activeRecordStringTemplate.setAttribute("validateProperty", "errorList = errorList.concat(this.validator.validate" + upperTheFirstCharacter(name) + "(((this." + name + " && Packages.airlift.util.FormatUtil.format(this." + name + ", \"" + dateMask + "\") + \"\")||\"\")));");
						}
						else
						{
							activeRecordStringTemplate.setAttribute("validateProperty", "errorList = errorList.concat(this.validator.validate" + upperTheFirstCharacter(name) + "(((this." + name + " && Packages.airlift.util.FormatUtil.format(this." + name + ") + \"\")||\"\")));");
						}
					}
				}
			}
			
			if (isArrayType(type) == true)
			{
				stringBufferStringTemplate.setAttribute("getterName", "airlift.util.AirliftUtil.generateStringFromArray(" + getterName + "())");
			}
			else
			{
				stringBufferStringTemplate.setAttribute("getterName", getterName + "()");
			}

			stringBufferStringTemplate.setAttribute("name", name);
		}

		Annotation auditable = new airlift.generator.Annotation();
		auditable.setName("airlift.generator.Auditable");

		if (_domainObjectModel.getDomainAnnotationSet().contains(auditable) == true)
		{
			activeRecordStringTemplate.setAttribute("auditGet", "if (\"on\".equalsIgnoreCase(AUDITING_GET) === true) { airlift.audit(this.json(), \"GET\", this.id); }");
			activeRecordStringTemplate.setAttribute("auditPut", "if (\"on\".equalsIgnoreCase(AUDITING_UPDATE) === true) { airlift.audit(this.json(), \"UPDATE\", this.id); }");
			activeRecordStringTemplate.setAttribute("auditPost", "if (\"on\".equalsIgnoreCase(AUDITING_INSERT) === true) { airlift.audit(this.json(), \"INSERT\", this.id); }");
			activeRecordStringTemplate.setAttribute("auditDelete", "if (\"on\".equalsIgnoreCase(AUDITING_DELETE) === true) { airlift.audit(this.json(), \"DELETE\", this.id); }");
		}
		
		activeRecordStringTemplate.setAttribute("attributeStringBufferAppends", stringBufferStringTemplate);
		
		return activeRecordStringTemplate.toString();
	}

	/**
	 * Generate validation object.
	 *
	 * @param _domainObjectModel the _domain object model
	 * @return the string
	 */
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
			
			if (nullable == false || "true".equalsIgnoreCase(isPrimaryKey) == true)
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

	/**
	 * Generate domain constructors.
	 *
	 * @param _elementNameToDomainObjectModelMap the _element name to domain object model map
	 * @return the string
	 */
	public String generateDomainConstructors(Map<String, DomainObjectModel> _elementNameToDomainObjectModelMap)
	{
		StringTemplate template = getStringTemplateGroup().getInstanceOf("airlift/language/javascript/DomainConstructors");
		boolean isHighLevelAttributesSet = false;

		for (DomainObjectModel domainObjectModel: _elementNameToDomainObjectModelMap.values())
		{
			if (domainObjectModel.isAbstractDomain() == false)
			{
				if (isHighLevelAttributesSet == false)
				{
					template.setAttribute("appName", domainObjectModel.getAppName());

					isHighLevelAttributesSet = true;
				}

				template.setAttribute("appNameMethod", domainObjectModel.getAppName());
				template.setAttribute("domainName", lowerTheFirstCharacter(domainObjectModel.getClassName()));
			}
		}

		return template.toString();
	}
}