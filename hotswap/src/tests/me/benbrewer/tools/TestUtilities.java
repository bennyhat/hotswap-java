package me.benbrewer.tools;

import java.lang.reflect.Field;

public class TestUtilities {
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField( Object obj, String fieldName, Class<T> type ) throws Exception {
		try {
			Field field = obj.getClass().getDeclaredField( fieldName );
			return getPrivateField( field, obj, fieldName, (Class<T>)field.getType() );
		} catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField( Class<?> cls, String fieldName, Class<T> type ) {
		try {
			Field field = cls.getDeclaredField( fieldName );
			return getPrivateField( field, null, fieldName, (Class<T>)field.getType() );
		} catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T setPrivateField( Object obj, String fieldName, T newValue ) throws Exception {
		Field field = obj.getClass().getDeclaredField( fieldName );
		T oldValue = getPrivateField( field, obj, fieldName, (Class<T>)field.getType() );
		setPrivateField( field, obj, fieldName, newValue );
		return oldValue;
	}
	@SuppressWarnings("unchecked")
	public static <T> T setPrivateField( Class<?> cls, String fieldName, T newValue ) throws Exception {
		Field field = cls.getDeclaredField( fieldName );
		T oldValue = getPrivateField( field, null, fieldName, (Class<T>)field.getType() );
		setPrivateField( field, null, fieldName, newValue );
		return oldValue;
	}
	private static <T> T getPrivateField( Field field, Object obj, String fieldName, Class<T> type ) throws Exception {
		boolean prev = field.isAccessible();
		field.setAccessible( true );
		@SuppressWarnings("unchecked")
		T value = (T)field.get( obj );
		field.setAccessible( prev );
		return value;
	}
	private static <T> void setPrivateField( Field field, Object obj, String fieldName, T newValue ) throws Exception {
		boolean prev = field.isAccessible();
		field.setAccessible( true );
		field.set( obj, newValue );
		field.setAccessible( prev );
	}
}
