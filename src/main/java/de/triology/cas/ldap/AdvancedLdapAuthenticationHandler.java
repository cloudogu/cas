/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.login.LoginException;
import org.jasig.cas.authentication.LdapAuthenticationHandler;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.SimplePrincipal;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;

/**
 *
 * @author Sebastian Sdorra
 */
public class AdvancedLdapAuthenticationHandler extends LdapAuthenticationHandler
{

  private Set<String> stripDNAttributes;

  public AdvancedLdapAuthenticationHandler(Authenticator authenticator)
  {
    super(authenticator);
  }

  public void setStripDNAttributes(Set<String> stripDNAttributes)
  {
    this.stripDNAttributes = stripDNAttributes;
  }

  @Override
  protected Principal createPrincipal(String username, LdapEntry ldapEntry) throws LoginException
  {
    logger.debug("handle dn attributes of user {}", username);
    Principal principal = super.createPrincipal(username, ldapEntry);
    if (stripDNAttributes != null && ! stripDNAttributes.isEmpty()) {
      Map<String,Object> attributes = new LinkedHashMap<String, Object>(principal.getAttributes());
      for ( String sdna : stripDNAttributes ){
        logger.trace("search dn attribute {}", sdna);
        handleDNAttribute(attributes, sdna);
      }
      principal = new SimplePrincipal(username, attributes);
    }
    return principal;
  }
  
  @SuppressWarnings("unchecked")
  private void handleDNAttribute(Map<String,Object> attributes, String attribute){
    Object value = attributes.get(attribute);
    logger.trace("handle dn attribute {}: {}", attribute, value);
    if (value != null){
      Object fixedValue;
      if ( value instanceof Collection){
        List values = new ArrayList();
        for ( Object o : (Collection) value ){
          values.add(fixRDNValue(o));
        }
        fixedValue = values;
      } else {
        fixedValue = fixRDNValue(value);
      }
      attributes.put(attribute, fixedValue);
      logger.debug("fixed dn attribute {}: {}", attribute, fixedValue);
    } else {
      logger.trace("dn attribute {} not found",attribute);
    }
  }
  
  static Object fixRDNValue(Object value)
  {
    Object result = value;
    if ( value instanceof String){
      String svalue = (String) value;
      int eqindex = svalue.indexOf('=');
      int coindex = svalue.indexOf(',');
      if ( eqindex > 0 && (coindex < 0 || eqindex < coindex) && svalue.length() > eqindex + 1 ){
        svalue = svalue.substring(eqindex + 1);
        coindex = svalue.indexOf(',');
        if (coindex > 0){
          result = svalue.substring(0, coindex);
        } else {
          result = svalue;
        }
      }
    }
    return result;
  }
  
}
