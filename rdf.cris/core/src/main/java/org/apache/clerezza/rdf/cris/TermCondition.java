/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.clerezza.rdf.cris;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author gamars
 */
public class TermCondition extends Condition {

  /**
   * The property to search for.
   */
  VirtualProperty property;
  private String value;

  
  public TermCondition(VirtualProperty property, String value) {
    this(property, value, 1.0f);
  }
  
  public TermCondition(VirtualProperty property, String value, Float boost) {
    this.property = property;
    this.value = value;
    this.boost = boost;
  }
  
  public TermCondition(UriRef uriRefProperty, String value) {
    this(uriRefProperty, value, 1.0f);
  }
  public TermCondition(UriRef uriRefProperty, String value, Float boost) {
    this(new PropertyHolder(uriRefProperty,false), value,boost);
  }
  
  @Override
  protected Query query() {
    TermQuery termQuery = new TermQuery(new Term("\""+property.getStringKey()+"\"", value));
    termQuery.setBoost(this.getBoost());
    return termQuery;
  }
}
