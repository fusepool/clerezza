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
package org.apache.clerezza.rdf.cris.lucene;

import java.util.List;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.cris.Condition;
import org.apache.clerezza.rdf.cris.FacetCollector;
import org.apache.clerezza.rdf.cris.ResourceFinder;
import org.apache.clerezza.rdf.cris.SortSpecification;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 * Creates an index of RDF resources and provides an interface to 
 * search for indexed resources.
 *
 * @author reto, tio, daniel
 */
public class GraphFinder extends ResourceFinder {

  @Override
  public List<NonLiteral> findResources(List<? extends Condition> conditions, SortSpecification sortSpecification, FacetCollector... facetCollectors) throws ParseException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
	

}
