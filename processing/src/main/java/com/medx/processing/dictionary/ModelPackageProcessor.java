package com.medx.processing.dictionary;

import static com.medx.processing.util.MirrorUtil.createTypeDescriptor;
import static com.medx.processing.util.MirrorUtil.filterDictTypes;
import static com.medx.processing.util.MirrorUtil.filterExecutableElements;
import static com.medx.processing.util.MirrorUtil.filterGetters;
import static com.medx.processing.util.MirrorUtil.mapAttributeDescriptors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import com.medx.framework.annotation.DictType;
import com.medx.framework.annotation.XmlDictionary;
import com.medx.framework.dictionary.DictionaryReader;
import com.medx.framework.dictionary.DictionaryWriter;
import com.medx.framework.dictionary.model.AttributeDescriptor;
import com.medx.framework.dictionary.model.Dictionary;
import com.medx.framework.dictionary.model.TypeDescriptor;

public class ModelPackageProcessor {
	private PackageElement modelPackage;
	private RoundEnvironment environment;
	
	private XmlDictionary xmlDictionary;
	//private JavaDictionary javaDictionary;
	
	private Map<String, TypeDescriptor> typeDescriptors = new HashMap<String, TypeDescriptor>();
	private Map<String, List<AttributeDescriptor>> attributeDescriptors = new HashMap<String, List<AttributeDescriptor>>();
	
	private Dictionary dictionary;
	private DictionaryManager dictionaryManager;
	
	public ModelPackageProcessor(PackageElement modelPackage, RoundEnvironment environment) {
		this.modelPackage = modelPackage;
		this.environment = environment;
		
		xmlDictionary = modelPackage.getAnnotation(XmlDictionary.class);
		//javaDictionary = modelPackage.getAnnotation(JavaDictionary.class);
	}

	public void process() throws ModelPackageProcessingException {
		prepareDescriptors();
		
		try {
			loadDictionary();
		} catch (Exception e) {
			throw new ModelPackageProcessingException("Failed to load dicionary", e, modelPackage);
		}
		
		populateDictionary();
		
		System.out.println("++++++++++++++ " + typeDescriptors.size());
		System.out.println("++++++++++++++ " + dictionary.getTypeDescriptors().size());
		
		System.out.println("-------------- " + typeDescriptors);
		System.out.println("++++++++++++++ " + dictionary.getTypeDescriptors());
		
		try {
			storeDictionary();
		} catch (Exception e) {
			throw new ModelPackageProcessingException("Failed to store dicionary", e, modelPackage);
		}
	}
	
	private void prepareDescriptors() {
		Set<? extends Element> allDictTypes = environment.getElementsAnnotatedWith(DictType.class);
		
		List<TypeElement> dictTypes = filterDictTypes(allDictTypes, modelPackage);
		
		for (TypeElement dictType : dictTypes) {
			String className = dictType.getQualifiedName().toString();
			
			typeDescriptors.put(className, createTypeDescriptor(dictType));
			
			List<ExecutableElement> methods = filterExecutableElements(dictType.getEnclosedElements());
			List<ExecutableElement> getters = filterGetters(methods);
			
			attributeDescriptors.put(className, mapAttributeDescriptors(getters, modelPackage));
		}
	}
	
	private void populateDictionary() {
		int nextId = dictionaryManager.getNextId(xmlDictionary.startId());
		nextId = populateTypeDescriptors(nextId);
		nextId = populateAttributeDescriptors(nextId);
	}
	
	private int populateTypeDescriptors(int nextId) {
		for (Map.Entry<String, TypeDescriptor> descEntry : typeDescriptors.entrySet()) {
			TypeDescriptor oldDesc = dictionaryManager.addTypeDescriptor(descEntry.getValue());
			
			if (oldDesc != descEntry.getValue()) {
				descEntry.setValue(oldDesc);
				//TODO generate warning if different types
			}
			else {
				descEntry.getValue().setId(nextId++);
				descEntry.getValue().setVersion(dictionary.getVersion());
			}
		}
		
		return nextId;
	}
	
	private int populateAttributeDescriptors(int nextId) {
		for (List<AttributeDescriptor> descList : attributeDescriptors.values()) {
			
			List<AttributeDescriptor> oldDescs = new ArrayList<AttributeDescriptor>();
			
			for (Iterator<AttributeDescriptor> iter = descList.iterator(); iter.hasNext(); ) {
				AttributeDescriptor desc = iter.next();
				
				AttributeDescriptor oldDesc = dictionaryManager.addAttributeDescriptor(desc);
				
				if (oldDesc != desc) { 
					iter.remove();
					oldDescs.add(oldDesc);
					//TODO generate warning if different types
				}
				else {
					desc.setId(nextId++);
					desc.setVersion(dictionary.getVersion());
				}
			}
			
			descList.addAll(oldDescs);
		}
		
		return nextId;
	}
	
	private void loadDictionary() throws JAXBException, SAXException, IOException {
		DictionaryReader dictionaryReader = new DictionaryReader();
		
		if ((new File(xmlDictionary.path()).exists()))
			dictionary = dictionaryReader.readDictionary(xmlDictionary.path());
		else
			dictionary = DictionaryManager.createEmptyDictionary();
		
		dictionaryManager = new DictionaryManager(dictionary);
	}
	
	private void storeDictionary() throws JAXBException {
		DictionaryWriter dictionaryWriter = new DictionaryWriter();
		dictionaryWriter.writeDictionary(xmlDictionary.path(), dictionary);
	}
}
