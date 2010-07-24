var airlift;

if (!airlift)
{
	airlift = {};
}
else if (typeof airlift != "object")
{
	throw new Error("airlift already exists and it is not an object");
}

airlift.d = function(_id)
{
	var string = ">>>>" + HANDLER_NAME + ":" + _id;
	airlift.p(string);
	CONTENT_CONTEXT.debug(string);
};

//ls - loadScript
airlift.ls = function(_scriptName)
{
	SCRIPTING.loadScript(_scriptName);
};


airlift.checkAllowed = function(_domainClassName, _domainName, _method, _isCollection)
{
	var isCollection = (airlift.isDefined(_isCollection) === true) ? _isCollection : false;
	var securityContext = new Packages.airlift.servlet.rest.RestfulSecurityContext();
	securityContext.allowed(_domainClassName, _domainName,  _method, REQUEST, isCollection); 
};

//ar - Active Record
airlift.ar = function(_domainName)
{
	var domainName = (airlift.isDefined(_domainName) === true) ? Packages.airlift.util.AirliftUtil.upperTheFirstCharacter(_domainName) : Packages.airlift.util.AirliftUtil.upperTheFirstCharacter(DOMAIN_NAME);
	
	if (airlift.isDefined(airlift["create" + domainName]) !== true)
	{
		airlift.ls("javascript/airlift/activerecord/" + domainName + ".js");
	}

	return airlift["create" + domainName]();
};

//t - create StringTemplate
airlift.t = function(_templateName, _locale)
{
	LOG.info("Creating template: " + template.name);
	var template = TEMPLATE.getInstanceOf(_templateName);
	
	var localeProperties = airlift.loadLocaleProperties(_locale);
	template.setAttribute("messages", localeProperties);

	return template;
};

//every - execution function on every member of collection
airlift.every = function(_collection, _function)
{
	if (airlift.isDefined(_collection) === true)
	{
		for (var item in Iterator(_collection))
		{
			_function(item);
		}
	}
};

//split - separate collection members by function evaluation of true
//and false.
airlift.split = function(_collection, _function)
{
	var first = airlift.l();
	var second = airlift.l();

	if (airlift.isDefined(_collection) === true)
	{
		for (var item in Iterator(_collection))
		{
			(_function(item) === true) ? first.add(item) : second.add(item);
		}
	}

	return [first, second];
};

//partition - separate collection into lists partitioned by _attribute
//value
airlift.partition = function(_collection, _attribute)
{
	var partitionMap = airlift.m();
	var orderedKeys = airlift.l();

	if (airlift.isDefined(_collection) === true)
	{
		for (var item in Iterator(_collection))
		{
			var value = item[_attribute];

			if (airlift.isDefined(value) === true)
			{
				var list = partitionMap.get(value);

				if (airlift.isDefined(list) === false)
				{
					list = airlift.l();
					partitionMap.put(value, list);
					orderedKeys.add(value);
				}

				list.add(item);
			}
		}
	}

	return [orderedKeys, partitionMap];
};

//l - create a new java.util.ArrayList
airlift.l = function(_list)
{
	var list = {};
	
	list.a = function(_value)
	{
		this.add(_value);
		return list;
	};

	list.g = function(_index)
	{
		return this.get(_index);
	};

	list.every = function(_function)
	{
		airlift.every(list, _function);
		return list;
	}
			
	list.partition = function(_attribute)
	{
		return airlift.partition(list, _attribute);
	};

	list.split = function(_function)
	{
		return airlift.split(list, _function);
	};

	list.__iterator__ = function()
	{
		var myIterator = {};

		myIterator.iterator = this.iterator();
		
		myIterator.next = function()
		{
			var next;
			
			if (myIterator.iterator.hasNext() === true)
			{
				next =  myIterator.iterator.next();
			}
			else
			{
				throw new StopIteration();
			}

			return next;
		}

		return myIterator;
	};

	list.i = function()
	{
		return Iterator(list);
	}

	list = new JavaAdapter(Packages.java.util.ArrayList, list);

	if (airlift.isDefined(_list) === true)
	{
		list.addAll(_list);
	}
	
	return list;
};

//s - create a new java.util.HashSet
airlift.s = function(_set)
{
	var set = {};
	
	set.a = function(_value)
	{
		this.add(_value);
		return this;
	};

	set.every = function(_function)
	{
		airlift.every(set, _function);
		return this;
	}

	set.partition = function(_attribute)
	{
		return airlift.partition(set, _attribute);
	};

	set.split = function(_function)
	{
		return airlift.split(set, _function);
	};

	set.__iterator__ = function()
	{
		var myIterator = {};

		myIterator.iterator = this.iterator();

		myIterator.next = function()
		{
			var next;

			if (myIterator.iterator.hasNext() === true)
			{
				next =  myIterator.iterator.next();
			}
			else
			{
				throw new StopIteration();
			}

			return next;
		}

		return myIterator;
	};

	set.i = function()
	{
		return Iterator(set);
	}

	set = new JavaAdapter(Packages.java.util.HashSet, set);

	if (airlift.isDefined(_set) === true)
	{
		set.addAll(_set);
	}

	return set;
};

//m - create a new java.util.HashMap
airlift.m = function(_map)
{
	var map = {};

	map.p = function(_key, _value)
	{
		this.put(_key, _value);
		return this;
	};

	map.everyKey = function(_function)
	{
		airlift.every(this.keySet(), _function);
		return this;
	}

	map.everyValue = function(_function)
	{
		airlift.every(this.values(), _function);
		return this;
	}

	map.partitionKeys = function(_attribute)
	{
		return airlift.partition(this.keySet(), _attribute);
	};

	map.splitKeys = function(_attribute)
	{
		return airlift.split(this.keySet(), _attribute);
	};

	map.partitionValues = function(_attribute)
	{
		return airlift.partition(this.values(), _attribute);
	};

	map.split = function(_attribute)
	{
		return airlift.split(this.values(), _attribute);
	};

	map.i = function()
	{
		return Iterator(this.entrySet().iterator());
	}

	map = new JavaAdapter(Packages.java.util.HashMap, map);

	if (airlift.isDefined(_map) === true)
	{
		map.putAll(_map);
	}

	return map;
};

//a - create a new java array of the specified type
airlift.a = function(_javaType, _size)
{
	return Packages.java.lang.reflect.Array.newInstance(_javaType, _size);
};

//g - create an id generator
airlift.g = function()
{
	return Packages.airlift.util.IdGenerator.generate();
};

//p - print using java.lang.System.out.println
airlift.p = function(_message)
{
	OUT.println(_message);
};

//mm - create a message manager
airlift.mm = function()
{
	return new Packages.airlift.MessageManager();
};

airlift.exists = function(_id, _domainName, _appName)
{
	var activeRecord = airlift.ar(_domainName, _appName);

	var id = (airlift.isDefined(_id) === true) ? _id : ID;

	return activeRecord.exists(id);
};

airlift.get = function(_id, _domainName, _appName)
{
	var activeRecord = airlift.ar(_domainName, _appName);

	var id = (airlift.isDefined(_id) === true) ? _id : ID;
	
	activeRecord.get(id);

	return activeRecord;
};

airlift.collect = function(_domainName, _request, _appName)
{
	var activeRecord, limit, offset, orderBy, list;

	activeRecord = airlift.ar(_domainName, _appName);

	var request = (airlift.isDefined(_request) === true) ? _request : REQUEST;
	
	limit = request.getParameterMap().get("limit");
	offset = request.getParameterMap().get("offset");
	orderBy = request.getParameterMap().get("orderBy");

	limit = (airlift.isDefined(limit) === true) ? limit[0] : 10;
	offset = (airlift.isDefined(offset) === true) ? offset[0] : 0;
	orderBy = (airlift.isDefined(orderBy) === true) ? orderBy[0] : activeRecord.retrievePrimaryKeyName() + " ASC";
	
	list = activeRecord.collect(offset, limit, orderBy);

	return [activeRecord, list];
};

//post - post an active record based on domain URI and rest context.
//Post accepts function that runs after syntactic validation but before
//insert into persistent store.
airlift.post = function(_domainName, _appName)
{
	var [activeRecord, errorMap] = airlift.populate(_domainName, _appName);

	if (errorMap.isEmpty() === true)
	{
		activeRecord[activeRecord.retrievePrimaryKeyName()] =  airlift.g();
	}
	
	if (arguments.length > 2)
	{
		for (var i = 2; i < arguments.length; i++)
		{
			arguments[i](activeRecord);
		}
	}

	activeRecord.error = (errorMap.isEmpty() === false) || (activeRecord.hasMessages() === true);

	if (activeRecord.error === false)
	{
		activeRecord.insert();
	}

	return [activeRecord, errorMap, activeRecord[activeRecord.retrievePrimaryKeyName()]];
};

airlift.put = function(_domainName, _appName)
{
	var [activeRecord, errorMap] = airlift.populate(_domainName, _appName);
	
	if (arguments.length > 2)
	{
		for (var i = 2; i < arguments.length; i++)
		{
			arguments[i](activeRecord);
		}
	}

	activeRecord.error = (errorMap.isEmpty() === false) || (activeRecord.hasMessages() === true);
	
	if (activeRecord.error === false)
	{
		activeRecord.update();
	}

	return [activeRecord, errorMap];
};

airlift.del = function(_domainName, _id, _appName)
{
	var activeRecord = airlift.ar(_domainName, _appName);

	activeRecord.id = (airlift.isDefined(_id) === true) ? _id : ID;
 
	activeRecord.del();
};

airlift["delete"] = function(_domainName, _id, _appName)
{
	airlift.del(_domainName, _id, _appName);
};

airlift.isDefined = function(_variable)
{
	var defined = (_variable !== null && _variable !== undefined);
	
	return defined;
};

airlift.guid = function()
{
	return airlift.g();
};

airlift.renderError = function(_errorMap)
{	
	var stringBuffer = java.lang.StringBuffer(); 

	for (var errorName in Iterator(_errorMap.keySet()))
	{
		var errors = _errorMap.get(errorName);

		for (var error in Iterator(errors))
		{
			stringBuffer.append(Packages.airlift.util.HtmlUtil.toRdfa(APP_NAME, error)).append("\n");
		}
	}

	return stringBuffer.toString();
};

airlift.ic = function(_className)
{
	importClass("Packages." + _className)
};

airlift.prepareUri = function(_id, _uri)
{
	var baseURI = (airlift.isDefined(_uri) === true) ? new Packages.java.lang.String(_uri) : URI;
	
	var uri = new Packages.java.lang.String(baseURI).replaceAll("\\/$", "");

	return uri + "/" + _id;
};

airlift.toJavaString = function(_string)
{
	return new Packages.java.lang.String(_string);
};

airlift.appender = function(_initialText, _delimiter)
{
	var delimiter = (airlift.isDefined(_delimiter) === true) ? _delimiter : ",";
	
	var template = airlift.t();
	template.setTemplate(_initialText + "$append; separator=\"" + delimiter +"\"$");

	var appender = {
		append: function(_appendee)
				 {
					 template.setAttribute("append", _appendee);
				 },
		toString: function()
				 {
					 return template.toString();
				 }
	};

	return appender;
};

airlift.getPropertyValue = function(_resourceName, _propertyName)
{
	return Packages.airlift.util.PropertyUtil.getInstance().getProperty(_resourceName, _propertyName);
};

airlift.isWhitespace = function(_string)
{
	return Packages.airlift.util.AirliftUtil.isWhitespace(_string);
};

airlift.mergeCollections = function()
{
	var collection = airlift.l();
	
	for (var i = 0; i < arguments.length; i++)
	{
		if (airlift.isDefined(arguments[i]) === true)
		{
			collection.addAll(arguments[i]);
		}
	}

	return collection;
};

airlift.canonicalUri = function(_domainName, _id)
{
	return BASE + "a/" + _domainName + "/" + _id;
};

airlift.copy = function(_source, _destination, _propertyArray)
{
	_propertyArray.forEach(function (_e, _i, _a) { _destination[_e] = _source[_e]; });
};