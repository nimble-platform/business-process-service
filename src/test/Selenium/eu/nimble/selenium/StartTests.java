package eu.nimble.selenium;

import org.reflections.Reflections;
import java.lang.reflect.Method;
import java.util.*;

public class StartTests {

    public static void main(String [] args) throws Exception{
        Reflections reflections = new Reflections("eu.nimble.selenium");

        List<Class<?>> classes = new ArrayList<Class<?>>(reflections.getSubTypesOf(SeleniumInterface.class));

        // Sort the classes according to their names
        Collections.sort(classes,new Comparator<Class<?>>(){
            @Override
            public int compare(Class<?> aClass, Class<?> t1) {
                return aClass.getName().compareTo(t1.getName());
            }
        });

        for(Class<?> c : classes){
            Object object = c.newInstance();
            Method [] allMethods = c.getDeclaredMethods();
            for (Method m : allMethods){
                m.setAccessible(true);
                Object o = m.invoke(object);
            }
        }

    //    Test0_PublishProduct test0_publishProduct = new Test0_PublishProduct();
    //    test0_publishProduct.execute();

    //    Test94_PPAP test94_ppap = new Test94_PPAP();
    //    test94_ppap.execute();

    //    Test95_PPAPResponse test95_ppapResponse = new Test95_PPAPResponse();
    //    test95_ppapResponse.execute();

    //    Test96_PPAPResponseView test96_ppapResponseView = new Test96_PPAPResponseView();
    //    test96_ppapResponseView.execute();
    }
}
