package io.cloudsoft.winrm4j.service.shell;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SendResponse", propOrder = {
    "desiredStream"
})
public class SendResponse {

    @XmlElement(name = "DesiredStream", required = true)
    protected DesiredStreamType desiredStream;

    /**
     * Gets the value of the desiredStream property.
     * 
     * @return
     *     possible object is
     *     {@link DesiredStreamType }
     *     
     */
    public DesiredStreamType getDesiredStream() {
        return desiredStream;
    }

    /**
     * Sets the value of the desiredStream property.
     * 
     * @param value
     *     allowed object is
     *     {@link DesiredStreamType }
     *     
     */
    public void setDesiredStream(DesiredStreamType value) {
        this.desiredStream = value;
    }

}