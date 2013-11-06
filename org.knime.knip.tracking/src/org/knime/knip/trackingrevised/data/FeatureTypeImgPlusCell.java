package org.knime.knip.trackingrevised.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.ExternalizerManager;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.feature.AbstractBasicFeatureType;

public class FeatureTypeImgPlusCell<T extends RealType<T>> extends
		AbstractBasicFeatureType<ImgPlus<T>> {

	private static volatile FeatureTypeImgPlusCell<? extends RealType<?>> instance;

	/**
	 * Returns the only instance of this class.
	 * 
	 * @return the only instance
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static FeatureTypeImgPlusCell<? extends RealType<?>> getInstance() {
		if (instance == null) {
			synchronized (FeatureTypeImgPlusCell.class) {
				if (instance == null) {
					instance = new FeatureTypeImgPlusCell();
				}
			}
		}
		return instance;
	}

	/**
	 * DO NOT USE THIS CONSTRUCTOR. use the {@link #getInstance()} method
	 * instead to get the singleton of this {@link AbstractBasicFeatureType}
	 * implementation.
	 * 
	 * @see #getInstance()
	 */
	public FeatureTypeImgPlusCell() {
		super("image plus cell", ImgPlus.class, ImgPlusValue.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isNumeric() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ImgPlus<T> convertValue(Object value) throws InvalidFeatureException {
		if (value == null) {
			return null;
		}
		if (value instanceof ImgPlus) {
			return (ImgPlus<T>) value;
		}
		return convertStringValue(value.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImgPlus<T> convertStringValue(String valueString)
			throws InvalidFeatureException {
		if (valueString == null || valueString.isEmpty()) {
			return null;
		}
		try {
			BufferedDataInputStream inStream = new BufferedDataInputStream(
					new ByteArrayInputStream(
							convertString2ByteArray(valueString)));
			Img<T> img = ExternalizerManager.<Img<T>> read(inStream);
			ImgPlusMetadata metadata = ExternalizerManager
					.<ImgPlusMetadata> read(inStream);
			// long[] min = new long[imgPlus.numDimensions()];
			// imgPlus.min(min);
			// System.out.println(Arrays.toString(min));
			//
			// // TODO: get this thing running
			// /****************
			// * NEED FILESTORE HERE
			// */
			return new ImgPlus<T>(img, metadata);
		} catch (final Exception e) {
			throw new InvalidFeatureException("Can not load image from String"
					+ e.getMessage(), e);
		}
	}

	@Override
	public ImgPlus<T> getDefaultValue() {
		return null;
	}

	@Override
	public DataType getCellType() {
		return ImgPlusCell.TYPE;
	}

	@Override
	public String convert2String(ImgPlus<T> value)
			throws InvalidFeatureException {
		if (value == null) {
			return null;
		}
		try {
			ByteArrayOutputStream streamByte = new ByteArrayOutputStream();
			BufferedDataOutputStream stream = new BufferedDataOutputStream(
					streamByte);
			ExternalizerManager.<Img<T>> write(stream, value.getImg());
			ExternalizerManager.<ImgPlusMetadata> write(stream,
					new DefaultImgMetadata(value), ImgPlusMetadata.class);
			
			stream.flush();
			return convertByteArray2String(streamByte.toByteArray());
		} catch (Exception e) {
			throw new InvalidFeatureException(
					"Error converting img to string: " + e.getMessage(), e);
		}
	}

	@Override
	public DataCell convert2Cell(FileStoreFactory fileStoreFactory,
			ImgPlus<T> value) throws InvalidFeatureException {
		if (value == null) {
			return DataType.getMissingCell();
		}
		try {
			ImgPlusCell<T> cell = new ImgPlusCellFactory(fileStoreFactory)
					.createCell(value);
			// System.out.println("Minimum of cell after: " +
			// Arrays.toString(((ImgPlusCell<T>)cell).getMinimum()));
			return cell;
		} catch (IOException e) {
			throw new InvalidFeatureException(e.getMessage());
		}
	}

	@Override
	protected DataCell convert2CellInternal(ImgPlus<T> value)
			throws InvalidFeatureException {
		throw new RuntimeException(new IllegalAccessException(
				"Convert2CellInternal may not be called in "
						+ getClass().toString()));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ImgPlus<T> convertCellInternal(DataCell cell)
			throws InvalidFeatureException {
		return ((ImgPlusCell<T>) cell).getImgPlus();
	}
}
