var util = require('airlift/util');
var javaArray = require('airlift/javaArray');

function Incoming(_web)
{
	var that = this;
	var res = require('airlift/resource').create(_web);
	
	var formatUtil = Packages.airlift.util.FormatUtil;

	var convertUtil = Packages.org.apache.commons.beanutils.ConvertUtils;
	var dateConverter = new Packages.org.apache.commons.beanutils.converters.DateConverter();
	dateConverter.setLocale(_web.getLocale());
	dateConverter.setTimeZone(java.util.TimeZone.getTimeZone(_web.getTimezone()));
	dateConverter.setPatterns(javaArray.stringArray(4, ['MM-dd-yyyy', 'MM/dd/yyyy', 'EEE, dd MMM yyyy HH:MM:ss z', "yyyy-MM-ddTHH:mm:ss.SSS'Z'"]));
	convertUtil.register(dateConverter, util.createClass("java.util.Date"));

	var validationError = function(_name, _message)
	{
		return {name: _name, message: _message, category: "validation"};
	};

	var isRequired = function(_errors, _metadata, _name, _value)
	{
		var error;

		if (_value === '' || _value === null || _value === undefined)
		{
			error = "This is a required field.";
		}

		return error;
	};

	var allowedValue = function(_errors, _metadata, _name, _value)
	{
		!_metadata.allowedValues[_value] && _errors.push(validationError(_name, "This value is not allowed."));

		return _errors;
	};

	var hasFormat = function(_errors, _metadata, _name, _value)
	{
		var format = new RegExp(_metadata.hasFormat);

		if (format.test(_value) === false)
		{
			_errors.push(validationError(_name, "This value is not in the specified format."));
		}

		return _errors;
	};

	var maxLength = function(_errors, _metadata, _name, _value)
	{
		if (_value.length > _metadata.maxLength)
		{
			_errors.push(validationError(_name, "This value has too many characters."));
		}

		return _errors;
	};

	var minLength = function(_errors, _metadata, _name, _value)
	{
		if (_value.length < _metadata.minLength)
		{
			_errors.push(validationError(_name, "This value has too few characters."));
		}

		return _errors;
	};

	var maxValue = function(_errors, _metadata, _name, _value)
	{
		if (util.hasValue(_metadata.maxValue) && (_value > _metadata.maxValue))
		{
			_errors.push(validationError(_name, "This value is bigger than the allowed maximum."));
		}

		return _errors;
	};

	var minValue = function(_errors, _metadata, _name, _value)
	{
		if (util.hasValue(_metadata.minValue) && (_value < _metadata.minValue))
		{
			_errors.push(validationError(_name, "This value is smaller than the allowed minimum."));
		}

		return _errors;
	};

	var validateString = function(_value, _name, _metadata)
	{
		var errors = [];
		var value = _value;

		if (_value !== undefined && _value !== null)
		{
			value = _value + '';
		}

		if (_metadata.nullable === false)
		{
			var message = isRequired(errors, _metadata, _name, value);
			message && errors.push(validationError(_name, message));
		}

		if (errors.length === 0)
		{
			if (util.isEmpty(_metadata.allowedValues) === false)
			{
				errors = allowedValue(errors, _metadata, _name, value);
			}

			if (_metadata.hasFormat)
			{
				errors = hasFormat(errors, _metadata, _name, value);
			}

			if (value)
			{
				errors = maxLength(errors, _metadata, _name, value); //limitation of a Blob attribute set by datastore
				errors = minLength(errors, _metadata, _name, value);
				errors = (_metadata.maxValue && maxValue(errors, _metadata, _name, value)) || errors;
				errors = (_metadata.minValue && minValue(errors, _metadata, _name, value)) || errors;
			}
		}

		return errors;
	};

	var validateBytes = function(_value, _name, _metadata)
	{
		var errors = [];
		var value = _value;

		if (_metadata.nullable === false)
		{
			var message = isRequired(errors, _metadata, _name, value);
			message && errors.push(validationError(_name, message));
		}

		if (errors.length === 0)
		{
			if (value)
			{
				errors = maxLength(errors, {maxLength: 1024000}, _name, value);
				errors = minLength(errors, _metadata, _name, value);
			}
		}

		return errors;
	};

	var validateDate = function(_value, _name, _metadata)
	{
		var errors = [];
		var value = _value;

		if (_metadata.nullable === false)
		{
			var message = isRequired(errors, _metadata, _name, value);
			message && errors.push(validationError(_name, message));
		}

		return errors;
	};

	var validateNumber = function(_value, _name, _metadata)
	{
		var errors = [];
		var value = _value;

		if (_metadata.nullable === false)
		{
			var message = isRequired(errors, _metadata, _name, value);
			message && errors.push(validationError(_name, message));
		}

		if (errors.length === 0)
		{
			if (util.isEmpty(_metadata.allowedValues) === false)
			{
				errors = allowedValue(errors, _metadata, _name, value + '');
			}

			if (value)
			{
				errors = (_metadata.maxValue && maxValue(errors, _metadata, _name, value)) || errors;
				errors = (_metadata.minValue && minValue(errors, _metadata, _name, value)) || errors;
			}
		}

		return errors;
	};

	var validateBoolean = function(_value, _name, _metadata)
	{
		var errors = [];
		var value = _value;

		if (_value !== undefined && _value !== null)
		{
			value = _value;
		}

		if (_metadata.nullable === false)
		{
			var message = isRequired(errors, _metadata, _name, value);
			message && errors.push(validationError(_name, message));
		}

		return errors;
	};

	var validateCollection = function(_value, _name, _metadata)
	{
		var errors = [];
		var collection = _value;

		if (_metadata.nullable === false)
		{
			var message = isRequired(errors, _metadata, _name, collection);
			message && errors.push(validationError(_name, message));
		}

		if (collection && errors.length === 0)
		{
			for (var item in Iterator(collection))
			{
				item = item + '';

				if (util.isEmpty(_metadata.allowedValues) === false)
				{
					errors = allowedValue(errors, _metadata, _name, item);
				}

				if (_metadata.hasFormat)
				{
					errors = hasFormat(errors, _metadata, _name, item);
				}

				if (item)
				{
					errors = maxLength(errors, _metadata, _name, item);
					errors = minLength(errors, _metadata, _name, item);
					errors = (_metadata.maxValue && maxValue(errors, _metadata, _name, item)) || errors;
					errors = (_metadata.minValue && minValue(errors, _metadata, _name, item)) || errors;
				}
			}
		}

		return errors;
	};

	this.createKey = function createKey(_resourceName, _id)
	{
		return Packages.com.google.appengine.api.datastore.KeyFactory.createKey(_resourceName, _id);
	};

	this.createEntity = function createEntity(_resourceName, _id)
	{
		return new Packages.com.google.appengine.api.datastore.Entity(this.createKey(_resourceName, _id));
	};

	this.bookkeeping = function bookkeeping(_entity, _userId, _postDate, _putDate)
	{
		var user = _web.getUser();
		var userId = _userId||user && user.getId()||'user id not provided';

		_entity.setProperty("auditUserId", userId);
		_entity.setProperty("auditRequestId", util.getWebRequestId());
		_entity.setProperty("auditPostDate", _postDate||util.createDate());
		_entity.setProperty("auditPutDate", _putDate||_entity.getProperty("auditPostDate"));
	};

	this.entify = function entify(_entity, _value, _attributeName, _resource, _attributeMetadata)
	{
		if (util.isEmpty(this.allErrors()) === true && "id".equalsIgnoreCase(_attributeName) === false)
		{
			var isIndexable = _attributeMetadata.isIndexable;
			var value = _value;
			var type = _attributeMetadata.type;
			
			if (util.hasValue(value) && util.hasValue(_attributeMetadata.mapTo) === false && util.hasValue(_attributeMetadata.mapToMany) === false)
			{
				if (type === 'java.lang.String')
				{
					//500 is the Google App Engine limitation for Strings
					//persisted to the datastore.
					if (_attributeMetadata.maxLength > 500)
					{
						isIndexable = false;
						value = new Packages.com.google.appengine.api.datastore.Text(value);
					}
				}
				else if (type === 'bytes')
				{
					isIndexable = false;
					value = new Packages.com.google.appengine.api.datastore.Blob(value);					
				}

				(isIndexable === true) ? _entity.setProperty(_attributeName, value) : _entity.setUnindexedProperty(_attributeName, value);
			}
			else if (util.hasValue(value) && util.hasValue(_attributeMetadata.mapTo) === true)
			{
				if (value && (value instanceof java.lang.String || typeof value === 'string'))
				{
					_entity.setProperty(_attributeName, value);
				}
				else
				{
					var embeddedEntity = new Packages.com.google.appengine.api.datastore.EmbeddedEntity();
					res.each(_attributeMetadata.mapTo, value, that.entify.partial(embeddedEntity));
					_entity.setUnindexedProperty(_attributeName, embeddedEntity);
				}
			}
			else if (util.hasValue(_attributeMetadata.mapToMany) === true)
			{
				util.info('map to many resource', JSON.stringify( _resource));
				util.info('map to many value is', _attributeName, value);
				util.info('map to many class is', _attributeName, value && value.getClass && value.getClass());
				
				if (value instanceof java.util.Collection === false) { throw 'Map to many property must be a java.util.Collection'; }
				
				if (value.isEmpty() === false)
				{
					var firstItem = value.get(0);
					
					if (firstItem instanceof java.lang.String === true)
					{
						_entity.setProperty(_attributeName, value);
					}
					else
					{
						var embeddedEntity;
						var embeddedEntityList = new Packages.java.util.ArrayList();
						
						for (var item in Iterator(value))
						{
							embeddedEntity = new Packages.com.google.appengine.api.datastore.EmbeddedEntity();
							res.each(_attributeMetadata.mapToMany, item, that.entify.partial(embeddedEntity));
							_entity.setUnindexedProperty(_attributeName, embeddedEntity);
							embeddedEntityList.add(embeddedEntity);
						}

						_entity.setUnindexedProperty(_attributeName, embeddedEntityList);
					}
				}
				else
				{
					_entity.setProperty(_attributeName, value);
				}
			}
		}
	};

	this.encrypt = function encrypt(_entity, _value, _attributeName, _resource, _attributeMetadata)
	{
		if (util.isEmpty(this.allErrors()) === true && _attributeMetadata.encrypted === true)
		{
			var password = _web.getServlet().getServletConfig().getInitParameter("a.cipher.password");
			var initialVector = _web.getServlet().getServletConfig().getInitParameter("a.cipher.initial.vector");
			var revolutions = _web.getServlet().getServletConfig().getInitParameter("a.cipher.revolutions")||20;

			var encryptedAttribute = new Packages.com.google.appengine.api.datastore.Blob(Packages.airlift.util.AirliftUtil.encrypt(Packages.airlift.util.AirliftUtil.convert(_entity.getProperty(_attributeName)||javaArray.byteArray(0)), password, initialVector, null, null, null, null, revolutions));
			var attributeEncryptedName = _attributeName + "Encrypted";

			_entity.setUnindexedProperty(attributeEncryptedName, encryptedAttribute);
			_entity.setUnindexedProperty(_attributeName, null);
		}
	};

	var convertToSingleValue = function(_parameterValue, _type, _index)
	{
		var value = _parameterValue && (util.isWhitespace(_parameterValue[_index]) === false) && util.trim(_parameterValue[_index]) || null;

		return convertUtil.convert(value, util.createClass(_type));
	};

	var convertToByteArray  = function(_parameterValue, _type, _index)
	{
		var value = _parameterValue && (util.isWhitespace(_parameterValue[_index]) === false) && util.trim(_parameterValue[_index]) || null;

		return value.getBytes('UTF-8');
	};
	
	var convertToMultiValue = function(_parameterValue, _collection)
	{
		if (_parameterValue)
		{
			for (var i = 0, length = _parameterValue.length; i < length; i++) { _collection.add(convertToSingleValue(_parameterValue, "java.lang.String", i)) }
		}

		return _collection;
	};

	function Converter()
	{
		this["java.lang.String"] = function(_parameterValue) { return convertToSingleValue(_parameterValue, "java.lang.String", 0); };
		this["java.lang.Integer"] = function(_parameterValue) { var value = convertToSingleValue(_parameterValue, "java.lang.Integer", 0); return util.primitive(value); };
		this["java.lang.Boolean"] = function(_parameterValue) { var value = convertToSingleValue(_parameterValue, "java.lang.Boolean", 0); return util.primitive(value); };
		this["java.lang.Long"] = function(_parameterValue) { var value = convertToSingleValue(_parameterValue, "java.lang.Long", 0); return util.primitive(value) };
		this["java.lang.Double"] = function(_parameterValue) { var value = convertToSingleValue(_parameterValue, "java.lang.Double", 0); return util.primitive(value) };
		this["java.lang.Float"] = function(_parameterValue) { var value = convertToSingleValue(_parameterValue, "java.lang.Float", 0); return util.primitive(value) };
		this["java.lang.Short"] = function(_parameterValue) { var value = convertToSingleValue(_parameterValue, "java.lang.Short", 0); return util.primitive(value) };

		this["java.util.Date"] = function(_parameterValue) { return convertToSingleValue(_parameterValue, "java.util.Date", 0); };

		this["bytes"] = function(_parameterValue) { return convertToByteArray(_parameterValue); };
		
		this["java.lang.Byte"] = function() { throw new Error("Airlift currently does not support java.lang.Byte. Try using String instead or file a feature request."); };
		this["java.lang.Character"] = function() { throw new Error("Airlift currently does not support java.lang.Character objects. Try using String instead or file a feature request."); };

		this["java.util.Set"] = function(_parameterValue) { var collection = new Packages.java.util.HashSet(); return convertToMultiValue(_parameterValue, collection); }
		this["java.util.HashSet"] = this["java.util.Set"];
		this["java.util.List"] = function(_parameterValue) { var collection = new Packages.java.util.ArrayList(); return convertToMultiValue(_parameterValue, collection); }
		this["java.util.ArrayList"] = this["java.util.List"];
		this["java.util.List<java.lang.String>"] = this["java.util.List"];
		this["java.util.ArrayList<java.lang.String>"] = this["java.util.List"];
		this["java.util.HashSet<java.lang.String>"] = this["java.util.Set"];
		this["java.util.Set<java.lang.String>"] = this["java.util.HashSet"];
	}

	function CollectionTypes()
	{
		this["java.util.Set"] = 1;
		this["java.util.HashSet"] = 1;
		this["java.util.List"] = 1;
		this["java.util.ArrayList"] = 1;
		this["java.util.List<java.lang.String>"] = 1;
		this["java.util.ArrayList<java.lang.String>"] = 1;
		this["java.util.HashSet<java.lang.String>"] = 1;
		this["java.util.Set<java.lang.String>"] = 1;
	}

	var converter = new Converter();
	var collectionTypes = new CollectionTypes();

	this.convert = function convert(_value, _attributeName, _resource, _attributeMetadata)
	{
		var request = _web.getRequest();

		var resourceName = this.resourceName;
		var value;

		var type = _attributeMetadata.type;

		if ("id".equals(_attributeName) !== true)
		{
			try
			{
				if (converter[type])
				{
				    var parameterValue = request.getParameterValues(_attributeName);

					if (util.hasValue(parameterValue) === false && collectionTypes[type])
					{
						parameterValue = request.getParameterValues(_attributeName + '[]');
					}

					value = (util.hasValue(parameterValue) && converter[type](parameterValue)) || null;
				}
				else
				{
					throw new Error('no converter found for type: ' + type);
				}
			}
			catch(e)
			{
				if (e.javaException)
				{
					util.warning(e.javaException.getMessage());
				}
				else
				{
					util.warning(e.message);
				}

				this.report(_attributeName, "This value is not correct.", "conversion");
			}
			
			if ((!value || util.isWhitespace(value) === true) && (_attributeMetadata.mapTo && util.isWhitespace(_attributeMetadata.mapTo) === false))
			{
				/* Form value overrides what is in the URI.  This is done for
				 * security reasons.  The foreign key may be protected via TLS
				 * by including it in the form and not in the URI.  If it is
				 * included in the form the expectation is that the URI should
				 * be overridden.
				 */

				var restContext = _web.getRestContext();
				var parameterValue = restContext.getParameter(_attributeMetadata.mapTo);
				value = (parameterValue && util.hasValue(parameterValue.get(0)) && parameterValue.get(0)) || null; //rest context parameters are always strings ...
			}

			/* There is no way to represent mapToMany in a URI
			 * therefore the form value is taken. 
			 */
		}
		else
		{
			var restContext = _web.getRestContext();
			var parameterValue = restContext.getParameter(resourceName);
			//convert only works for the first id.  Multiple puts not
			//supported at this time - Bediako
			value = (parameterValue && util.hasValue(parameterValue.get(0)) && parameterValue.get(0)) || null; //rest context parameters are always strings ...
		}
		
		_resource[_attributeName] = value;

		var res = require('airlift/resource').create(_web);

		return value;
	};

	function Validator()
	{
		this["java.lang.String"] = function(_value, _attributeName, _attributesMetadata, _reportErrors) { var errors = validateString(_value, _attributeName, _attributesMetadata); errors.length && _reportErrors(_attributeName, errors); };
		this["java.lang.Boolean"] = function(_value, _attributeName, _attributesMetadata, _reportErrors) { var errors = validateBoolean(_value, _attributeName, _attributesMetadata); errors.length && _reportErrors(_attributeName, errors); };
		this["java.lang.Integer"] = function(_value, _attributeName, _attributesMetadata, _reportErrors) { var errors = validateNumber(_value, _attributeName, _attributesMetadata); errors.length && _reportErrors(_attributeName, errors); };

		this["java.lang.Double"] = this["java.lang.Integer"];
		this["java.lang.Long"] = this["java.lang.Integer"];
		this["java.lang.Short"] = this["java.lang.Integer"];
		this["java.lang.Float"] = this["java.lang.Integer"];

		this["bytes"] = function(_value, _attributeName, _attributesMetadata, _reportErrors) { var errors = validateBytes(_value, _attributeName, _attributesMetadata); errors.length && _reportErrors(_attributeName, errors); };
		
		this["java.lang.Character"] = function() { throw new Error("Airlift currently does not support java.lang.Character. Try using String instead or file a feature request."); };
		this["java.lang.Byte"] = function() { throw new Error("Airlift currently does not support java.lang.Byte. Try using String instead or file a feature request."); };

		this["java.util.Date"] = function(_value, _attributeName, _attributesMetadata, _reportErrors) { var errors = validateDate(_value, _attributeName, _attributesMetadata); errors.length && _reportErrors(_attributeName, errors); };

		this["java.util.List"] = function(_value, _attributeName, _attributesMetadata, _reportErrors) { var errors = validateCollection(_value, _attributeName, _attributesMetadata); errors.length && _reportErrors(_attributeName, errors); };
		this["java.util.Set"] = this["java.util.List"];
		this["java.util.ArrayList"] = this["java.util.List"];
		this["java.util.HashSet"] = this["java.util.List"];

		this["java.util.List<java.lang.String>"] = this["java.util.List"];
		this["java.util.ArrayList<java.lang.String>"] = this["java.util.List"];
		this["java.util.HashSet<java.lang.String>"] = this["java.util.List"];
		this["java.util.Set<java.lang.String>"] = this["java.util.List"];
	}

	var validator = new Validator();

	this.validate = function validate(_value, _name, _resource, _metadata)
	{
		var type = _metadata.type;

		if (!!validator[type] === true)
		{
			validator[type](_value, _name, _metadata, this.report);
		}
		else
		{
			throw 'no validator found for type: ' + type;
		}

		return _value;
	};
}

exports.create = function(_web)
{
	if (!_web) { throw 'Unable to create incoming module without an airlift/web object' }

	return new Incoming(_web);
};