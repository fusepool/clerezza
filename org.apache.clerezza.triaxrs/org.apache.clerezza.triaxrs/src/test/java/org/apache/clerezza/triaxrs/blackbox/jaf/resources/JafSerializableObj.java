/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.triaxrs.blackbox.jaf.resources;

import java.io.Serializable;

/**
 * 
 * @author mir
 */
public class JafSerializableObj implements Serializable{
	private String field1;
	private String field2;
	
	public JafSerializableObj(String field1,String field2){
		this.field1 = field1;
		this.field2 = field2;
	}
	
	public String getField1(){
		return field1;
	}
	
	public String getField2(){
		return field2;
	}
	
	@Override
	public String toString(){
		return "Field1: " + field1 + " Field2: " + field2;
	}
}
