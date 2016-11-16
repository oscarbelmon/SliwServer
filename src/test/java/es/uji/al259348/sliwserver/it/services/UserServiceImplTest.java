package es.uji.al259348.sliwserver.it.services;

import es.uji.al259348.sliwserver.Main;
import es.uji.al259348.sliwserver.ml.TrainingSetBuilder;
import es.uji.al259348.sliwserver.model.Location;
import es.uji.al259348.sliwserver.model.Sample;
import es.uji.al259348.sliwserver.model.User;
import es.uji.al259348.sliwserver.repositories.SampleRepository;
import es.uji.al259348.sliwserver.services.MLService;
import es.uji.al259348.sliwserver.services.MLServiceImpl;
import es.uji.al259348.sliwserver.services.UserService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
//import org.springframework.classify.annotation.Classifier;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Debug;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Main.class)
public class UserServiceImplTest {

    @Autowired
    UserService userService;

    @Autowired
    SampleRepository sampleRepository;

    @BeforeClass
    public static void setUp() {

    }

//    @Test
    public void configure() {

        User user = userService.getUser("1");
        List<Sample> samples = sampleRepository.findByUserIdAndValid(user.getId(), true);

        userService.configure(user, samples);

    }

//    @Test
    public void encuentraUsuario() {
        User user = userService.getUser("268b0027-82f1-4a71-9874-ddd5ed6b96bb");
        List<Classifier> classifiers = MLServiceImpl.fromBase64(user.getClassifiers());
        classifiers.stream()
                .forEach(System.out::println);
    }

//    @Test
    public void encuentraMuestrasUsuario() {
        User user = userService.getUser("95a441ef-40c7-44bb-b92d-8e5d87845297");
        List<Sample> noValidadas = sampleRepository.findByUserIdAndValid(user.getId(), false);
        System.out.println("Muestras no validadas: " + noValidadas.size());
        List<Sample> samples = sampleRepository.findByUserIdAndValid(user.getId(), true);
        System.out.println("Muestras validadas: " + samples.size());
//        MLService mlService = new MLServiceImpl();
//        System.out.println(mlService.classify(user, samples.get(0)));
//        System.out.println(samples.get(0).getLocation());
//        samples.stream()
//                .forEach(sample -> {
//                    System.out.println("Classfied: " + mlService.classify(user, sample));
//                    System.out.println("Real: " + sample.getLocation());
//                });


    }

    @Test
    public void matrizDeConfusion() {
        User user = userService.getUser("268b0027-82f1-4a71-9874-ddd5ed6b96bb");
        List<Classifier> classifiers = MLServiceImpl.fromBase64(user.getClassifiers());
        user.getLocations().stream()
                .map(Location::getName)
                .collect(Collectors.toList())
                .forEach(System.out::println);

        Instances instances = new TrainingSetBuilder()
                .setAttributes(user.getBssids())
                .setClassAttribute("Location", user.getLocations().stream()
                        .map(Location::getName)
                        .collect(Collectors.toList())
                )
                .build("TrainingSet", 1);

        MLServiceImpl mlService = new MLServiceImpl();


        List<Sample> samples = sampleRepository.findByUserIdAndValid(user.getId(), true);
        // Create instance
        for(Sample sample: samples) {
            Map<String, Integer> BSSIDLevelMap = mlService.getBSSIDLevelMap(sample);

            Instance instance = new Instance(instances.numAttributes());

            for (Enumeration e = instances.enumerateAttributes(); e.hasMoreElements(); ) {
                Attribute attribute = (Attribute) e.nextElement();
                String bssid = attribute.name();
                int level = (BSSIDLevelMap.containsKey(bssid)) ? BSSIDLevelMap.get(bssid) : 0;
                instance.setValue(attribute, level);
            }

            if (sample.getLocation() != null)
                instance.setValue(instances.classAttribute(), sample.getLocation());

            instance.setDataset(instances);
            instances.add(instance);
        }



        System.out.println(instances.numInstances());
        try {
//            Evaluation evaluation= new Evaluation(instances);
            Random rand = new Random(1);
            int folds = 10;
            for(Classifier classifier: classifiers) {
                System.out.println(classifier.getClass().getName());
                Evaluation evaluation= new Evaluation(instances);
                evaluation.crossValidateModel(classifier, instances, folds, rand);
                System.out.println(evaluation.toMatrixString());
                System.out.println(evaluation.toSummaryString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}