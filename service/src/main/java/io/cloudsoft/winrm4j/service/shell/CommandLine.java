
package io.cloudsoft.winrm4j.service.shell;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommandLine", propOrder = {
    "command",
    "arguments"
})
public class CommandLine {

    @XmlElement(name = "Command")
    protected String command;
    @XmlElement(name = "Arguments")
    protected List<String> arguments;

    public String getCommand() {
        return command;
    }

    public void setCommand(String value) {
        this.command = value;
    }

    public List<String> getArguments() {
        if (arguments == null) {
            arguments = new ArrayList<String>();
        }
        return this.arguments;
    }

}
