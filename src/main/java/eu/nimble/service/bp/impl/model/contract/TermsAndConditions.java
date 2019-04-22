package eu.nimble.service.bp.impl.model.contract;

import java.util.ArrayList;
import java.util.List;

public class TermsAndConditions{

    private List<TermsAndConditionsSection> sections = new ArrayList<>();

    public TermsAndConditions() {
    }

    public List<TermsAndConditionsSection> getSections() {
        return sections;
    }

    public void setSections(List<TermsAndConditionsSection> sections) {
        this.sections = sections;
    }

    public void addParagraph(String paragraphName, String paragraphValue, List<String> parameters, List<String> defaultValues){
        TermsAndConditionsSection termsAndConditionsSection = new TermsAndConditionsSection();
        termsAndConditionsSection.setName(paragraphName);
        termsAndConditionsSection.setText(paragraphValue);
        termsAndConditionsSection.setParameters(parameters);
        termsAndConditionsSection.setDefaultValues(defaultValues);

        sections.add(termsAndConditionsSection);
    }

    public class TermsAndConditionsSection{
        private String name;
        private String text;
        private List<String> parameters;
        private List<String> defaultValues;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }

        public List<String> getDefaultValues() {
            return defaultValues;
        }

        public void setDefaultValues(List<String> defaultValues) {
            this.defaultValues = defaultValues;
        }
    }
}
