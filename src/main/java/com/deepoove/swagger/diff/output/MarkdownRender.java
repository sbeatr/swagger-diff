package com.deepoove.swagger.diff.output;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.*;

import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

public class MarkdownRender implements Render {

    final String H3 = "### ";
    final String H2 = "## ";
    final String BLOCKQUOTE = "> ";
    final String CODE = "`";
    final String PRE_CODE = "    ";
    final String PRE_LI = "    ";
    final String LI = "* ";
    final String HR = "---\n";

    public MarkdownRender() {}

    public String render(SwaggerDiff diff) {
        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        String ol_newEndpoint = ol_newEndpoint(newEndpoints);

        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        String ol_missingEndpoint = ol_missingEndpoint(missingEndpoints);

        List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
        String ol_changed = ol_changed(changedEndpoints);

        return renderHtml(diff.getOldVersion(), diff.getNewVersion(), ol_newEndpoint, ol_missingEndpoint, ol_changed);
    }

    public String renderHtml(String oldVersion, String newVersion, String ol_new, String ol_miss,
                             String ol_changed) {
        StringBuffer sb = new StringBuffer();
        sb.append(H2).append("Version " + oldVersion + " to " + newVersion).append("\n").append(HR);
        sb.append(H3).append("What's New").append("\n").append(HR)
                .append(ol_new).append("\n").append(H3)
                .append("What's Deprecated").append("\n").append(HR)
                .append(ol_miss).append("\n").append(H3)
                .append("What's Changed").append("\n").append(HR)
                .append(ol_changed);
        return sb.toString();
    }

    private String ol_newEndpoint(List<Endpoint> endpoints) {
        if (null == endpoints) return "";
        StringBuffer sb = new StringBuffer();
        for (Endpoint endpoint : endpoints) {
            sb.append(li_newEndpoint(endpoint.getMethod().toString(),
                    endpoint.getPathUrl(), endpoint.getSummary()));
        }
        return sb.toString();
    }

    private String li_newEndpoint(String method, String path, String desc) {
        StringBuffer sb = new StringBuffer();
        sb.append(LI).append(CODE).append(method).append(CODE)
                .append(" " + path).append(" " + desc + "\n");
        return sb.toString();
    }

    private String ol_missingEndpoint(List<Endpoint> endpoints) {
        if (null == endpoints) return "";
        StringBuffer sb = new StringBuffer();
        for (Endpoint endpoint : endpoints) {
            sb.append(li_newEndpoint(endpoint.getMethod().toString(),
                    endpoint.getPathUrl(), endpoint.getSummary()));
        }
        return sb.toString();
    }

    private String ol_changed(List<ChangedEndpoint> changedEndpoints) {
        if (null == changedEndpoints) return "";
        StringBuffer sb = new StringBuffer();
        for (ChangedEndpoint changedEndpoint : changedEndpoints) {
            String pathUrl = changedEndpoint.getPathUrl();
            Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint
                    .getChangedOperations();
            for (Entry<HttpMethod, ChangedOperation> entry : changedOperations
                    .entrySet()) {
                String method = entry.getKey().toString();
                ChangedOperation changedOperation = entry.getValue();
                String desc = changedOperation.getSummary();

                StringBuffer ul_detail = new StringBuffer();
                if (changedOperation.isDiffParam()) {
                    ul_detail.append(PRE_LI).append("Parameters")
                            .append(ul_param(changedOperation));
                }
                if (changedOperation.isDiffProp()) {
                    ul_detail.append(PRE_LI).append("Return Type")
                            .append(ul_response(changedOperation));
                }
                if (changedOperation.isDiffProduces()) {
                    ul_detail.append(PRE_LI).append("Produces")
                            .append(ul_produce(changedOperation));
                }
                if (changedOperation.isDiffConsumes()) {
                    ul_detail.append(PRE_LI).append("Consumes")
                            .append(ul_consume(changedOperation));
                }
                sb.append(CODE).append(method).append(CODE)
                        .append(" " + pathUrl).append(" " + desc + "  \n")
                        .append(ul_detail);
            }
        }
        return sb.toString();
    }

    private String ul_response(ChangedOperation changedOperation) {
        List<ElProperty> addProps = changedOperation.getAddProps();
        List<ElProperty> delProps = changedOperation.getMissingProps();
        List<ElProperty> changedProps = changedOperation.getChangedProps();
        StringBuffer sb = new StringBuffer("\n\n");

        String prefix = PRE_LI + PRE_CODE;
        for (ElProperty prop : addProps) {
            sb.append(PRE_LI).append(PRE_CODE).append(li_addProp(prop) + "\n");
        }
        for (ElProperty prop : delProps) {
            sb.append(prefix).append(li_missingProp(prop) + "\n");
        }
        for (ElProperty prop : changedProps) {
            sb.append(prefix).append(li_changedProp(prop) + "\n");
        }
        return sb.toString();
    }

    private String li_missingProp(ElProperty prop) {
        Property property = prop.getProperty();
        StringBuffer sb = new StringBuffer("");
        sb.append("Deleted: ").append(prop_string(prop));
        return sb.toString();
    }


    private String prop_string(ElProperty prop) {
        StringBuffer sb = new StringBuffer("");
        Property property = prop.getProperty();
        String desc = property.getDescription();
        String description = null == desc ? "" : ("Description: \"" + desc + "\"" );
        sb.append("          Field: " + prop.getEl() + "\n          ")
                .append("Type: " + property.getType() + "\n          ")
                .append(description);
        return sb.toString();

    }

    private String li_addProp(ElProperty prop) {
        StringBuffer sb = new StringBuffer("");
        sb.append("Added: \n").append(prop_string(prop));
        return sb.toString();
    }

    private String li_changedProp(ElProperty prop) {
        Property property = prop.getProperty();
        Property new_property = prop.getNewProperty();

        String modified = "Modified: \n";
        String ws = "          ";

        StringBuffer type_string = new StringBuffer("");
        StringBuffer desc_string = new StringBuffer("");

        if (prop.isTypeChange()) {
            type_string.append(ws).append("Type changed from " + property.getType() + " to " + new_property.getType());
        }

        if (prop.isDescriptionChange()) {
            desc_string.append(ws).append("Description changed from \"" + property.getDescription() + "\" to \"" + new_property.getDescription() + "\"");
        }

        StringBuffer sb = new StringBuffer("");
        sb.append(modified).append(type_string).append("\n").append(desc_string);

        return sb.toString();
    }

    private String ul_param(ChangedOperation changedOperation) {
        List<Parameter> addParameters = changedOperation.getAddParameters();

        List<Parameter> delParameters = changedOperation.getMissingParameters();
        List<ChangedParameter> changedParameters = changedOperation
                .getChangedParameter();
        StringBuffer sb = new StringBuffer("\n\n");
        for (Parameter param : addParameters) {
            sb.append(PRE_LI).append(PRE_CODE)
                    .append(li_addParam(param) + "\n");
        }
        for (ChangedParameter param : changedParameters) {
            List<ElProperty> increased = param.getIncreased();
            for (ElProperty prop : increased) {
                sb.append(PRE_LI).append(PRE_CODE)
                        .append(li_addProp(prop) + "\n");
            }
        }
        for (ChangedParameter param : changedParameters) {
            boolean changeRequired = param.isChangeRequired();
            boolean changeDescription = param.isChangeDescription();
            if (changeRequired || changeDescription) sb.append(PRE_LI)
                    .append(PRE_CODE).append(li_changedParam(param) + "\n");
        }
        for (ChangedParameter param : changedParameters) {
            List<ElProperty> missing = param.getMissing();
            List<ElProperty> changed = param.getChanged();
            for (ElProperty prop : missing) {
                sb.append(PRE_LI).append(PRE_CODE)
                        .append(li_missingProp(prop) + "\n");
            }
            for (ElProperty prop : changed) {
                sb.append(PRE_LI).append(PRE_CODE)
                        .append(li_changedProp(prop) + "\n");
            }
        }
        for (Parameter param : delParameters) {
            sb.append(PRE_LI).append(PRE_CODE)
                    .append(li_missingParam(param) + "\n");
        }
        return sb.toString();
    }

    private String li_addParam(Parameter param) {
        StringBuffer sb = new StringBuffer("");
        sb.append("Add ").append(param.getName())
                .append(null == param.getDescription() ? ""
                        : (" //" + param.getDescription()));
        return sb.toString();
    }

    private String li_missingParam(Parameter param) {
        StringBuffer sb = new StringBuffer("");
        sb.append("Delete ").append(param.getName())
                .append(null == param.getDescription() ? ""
                        : (" //" + param.getDescription()));
        return sb.toString();
    }

    private String li_changedParam(ChangedParameter changeParam) {
        boolean changeRequired = changeParam.isChangeRequired();
        boolean changeDescription = changeParam.isChangeDescription();
        Parameter rightParam = changeParam.getRightParameter();
        Parameter leftParam = changeParam.getLeftParameter();
        StringBuffer sb = new StringBuffer("");
        sb.append(rightParam.getName());
        if (changeRequired) {
            sb.append(" change into " + (rightParam.getRequired() ? "required" : "not required"));
        }
        if (changeDescription) {
            sb.append(" Notes ").append(leftParam.getDescription()).append(" change into ")
                    .append(rightParam.getDescription());
        }
        return sb.toString();
    }

    private String ul_produce(ChangedOperation changedOperation) {
        List<String> addProduce = changedOperation.getAddProduces();
        List<String> delProduce = changedOperation.getMissingProduces();
        StringBuffer sb = new StringBuffer("\n\n");

        String prefix = PRE_LI + PRE_CODE;
        for (String mt : addProduce) {
            sb.append(PRE_LI).append(PRE_CODE).append(li_addMediaType(mt) + "\n");
        }
        for (String mt : delProduce) {
            sb.append(prefix).append(li_missingMediaType(mt) + "\n");
        }
        return sb.toString();
    }

    private String ul_consume(ChangedOperation changedOperation) {
        List<String> addConsume = changedOperation.getAddConsumes();
        List<String> delConsume = changedOperation.getMissingConsumes();
        StringBuffer sb = new StringBuffer("\n\n");

        String prefix = PRE_LI + PRE_CODE;
        for (String mt : addConsume) {
            sb.append(PRE_LI).append(PRE_CODE).append(li_addMediaType(mt) + "\n");
        }
        for (String mt : delConsume) {
            sb.append(prefix).append(li_missingMediaType(mt) + "\n");
        }
        return sb.toString();
    }

    private String li_missingMediaType(String type) {
        StringBuffer sb = new StringBuffer("");
        sb.append("Delete ").append(type);
        return sb.toString();
    }

    private String li_addMediaType(String type) {
        StringBuffer sb = new StringBuffer("");
        sb.append("Insert ").append(type);
        return sb.toString();
    }
}
