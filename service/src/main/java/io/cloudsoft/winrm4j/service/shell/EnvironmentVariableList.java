
package io.cloudsoft.winrm4j.service.shell;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EnvironmentVariableList", propOrder = {
    "variable"
})
public class EnvironmentVariableList {

    @XmlElement(name = "Variable", required = true)
    protected List<EnvironmentVariable> variable;

    /**
     * Gets the value of the variable property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the variable property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVariable().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EnvironmentVariable }
     * 
     * 
     */
    public List<EnvironmentVariable> getVariable() {
        if (variable == null) {
            variable = new ArrayList<EnvironmentVariable>();
        }
        return this.variable;
    }

}
