package org.franca.deploymodel.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ContentHandler;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.URIHandler;

public class DeploymentURIConverter implements URIConverter {

	URIConverter mOrigURICOnverter = null;
	String mOutDirectory = null;
	
	public DeploymentURIConverter(URIConverter origURICOnverter, String outDirectory)
	{
		mOrigURICOnverter = origURICOnverter;
		mOutDirectory = outDirectory;
	}

	@Override
	public URI normalize(URI uri) {
		return mOrigURICOnverter.normalize(uri);
	}

	@Override
	public Map<URI, URI> getURIMap() {
		return mOrigURICOnverter.getURIMap();
	}

	@Override
	public EList<URIHandler> getURIHandlers() {
		return mOrigURICOnverter.getURIHandlers();
	}

	@Override
	public URIHandler getURIHandler(URI uri) {
		return mOrigURICOnverter.getURIHandler(uri);
	}

	@Override
	public EList<ContentHandler> getContentHandlers() {
		return mOrigURICOnverter.getContentHandlers();
	}

	@Override
	public InputStream createInputStream(URI uri) throws IOException {
		return mOrigURICOnverter.createInputStream(uri);
	}

	@Override
	public InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException {
		return mOrigURICOnverter.createInputStream(uri, options);
	}

	@Override
	public OutputStream createOutputStream(URI uri) throws IOException {
		URI tmpURI = null;
		
		if (uri.isPlatform())
		{
			//in case of using plugin internal models, e.g. some_ip then copy the resource to the output directory too
			tmpURI = URI.createFileURI(mOutDirectory + uri.lastSegment());
		}
		else
		{
			tmpURI = URI.createFileURI(mOutDirectory + uri.path());
		}
		System.out.println("Saving " + tmpURI);
		return mOrigURICOnverter.createOutputStream(tmpURI);
	}

	@Override
	public OutputStream createOutputStream(URI uri, Map<?, ?> options)
			throws IOException {
		System.out.println("Saving " + URI.createFileURI(mOutDirectory + uri.path()));
		return mOrigURICOnverter.createOutputStream(URI.createFileURI(mOutDirectory + uri.path()), options);
	}

	@Override
	public void delete(URI uri, Map<?, ?> options) throws IOException {
		mOrigURICOnverter.delete(uri, options);
	}

	@Override
	public Map<String, ?> contentDescription(URI uri, Map<?, ?> options)
			throws IOException {
		return mOrigURICOnverter.contentDescription(uri, options);
	}

	@Override
	public boolean exists(URI uri, Map<?, ?> options) {
		return mOrigURICOnverter.exists(uri, options);
	}

	@Override
	public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
		return mOrigURICOnverter.getAttributes(uri, options);
	}

	@Override
	public void setAttributes(URI uri, Map<String, ?> attributes,
			Map<?, ?> options) throws IOException {
		mOrigURICOnverter.setAttributes(uri, attributes, options);
	}
}