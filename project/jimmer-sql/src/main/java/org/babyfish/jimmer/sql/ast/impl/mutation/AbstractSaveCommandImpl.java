package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.mutation.AbstractSaveCommand;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import javax.persistence.OneToMany;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class AbstractSaveCommandImpl<C extends AbstractSaveCommand<C>> implements AbstractSaveCommand<C> {

    SqlClient sqlClient;

    Data data;

    AbstractSaveCommandImpl(SqlClient sqlClient, Data data) {
        this.sqlClient = sqlClient;
        this.data = data != null ? data.freeze() : new Data(sqlClient).freeze();
    }

    @SuppressWarnings("unchecked")
    @Override
    public C configure(Consumer<Cfg> block) {
        Data newData = new Data(data);
        block.accept(newData);
        if (newData.mode == Mode.UPSERT &&
                newData.keyPropMultiMap.isEmpty() &&
                newData.autoDetachingSet.isEmpty() &&
                newData.autoAttachingSet.isEmpty()) {
            return (C)this;
        }
        return create(newData);
    }

    abstract C create(Data data);

    static class Data implements Cfg {

        private SqlClient sqlClient;

        private boolean frozen;

        private Mode mode;

        private Map<ImmutableType, Set<ImmutableProp>> keyPropMultiMap;

        private Set<ImmutableProp> autoAttachingSet;

        private Set<ImmutableProp> autoDetachingSet;

        Data(SqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.mode = Mode.UPSERT;
            this.keyPropMultiMap = new LinkedHashMap<>();
            this.autoAttachingSet = new LinkedHashSet<>();
            this.autoDetachingSet = new LinkedHashSet<>();
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.mode = Mode.UPSERT;
            this.keyPropMultiMap = new LinkedHashMap<>(base.keyPropMultiMap);
            this.autoAttachingSet = new LinkedHashSet<>(base.autoAttachingSet);
            this.autoDetachingSet = new LinkedHashSet<>(base.autoDetachingSet);
        }

        public SqlClient getSqlClient() {
            return sqlClient;
        }

        public Mode getMode() {
            return mode;
        }

        public Map<ImmutableType, Set<ImmutableProp>> getKeyPropMultiMap() {
            return keyPropMultiMap;
        }

        public Set<ImmutableProp> getAutoAttachingSet() {
            return autoAttachingSet;
        }

        public Set<ImmutableProp> getAutoDetachingSet() {
            return autoDetachingSet;
        }

        @Override
        public Cfg setMode(Mode mode) {
            validate();
            this.mode = Objects.requireNonNull(mode, "mode cannot be null");
            return this;
        }

        @Override
        public Cfg setKeyProps(ImmutableProp ... props) {
            validate();
            ImmutableType type = null;
            Set<ImmutableProp> set = new LinkedHashSet<>();
            for (ImmutableProp prop : props) {
                if (prop != null) {
                    if (prop.isId()) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' cannot be key property because it is id property"
                        );
                    } else if (prop.isAssociation() || !(prop.getStorage() instanceof Column)) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' cannot be key property because it is not a scalar property with storagy"
                        );
                    }
                    if (type == null) {
                        type = prop.getDeclaringType();
                    } else if (type != prop.getDeclaringType()) {
                        throw new IllegalArgumentException("key properties must belong to one type");
                    }
                    set.add(prop);
                }
            }
            if (type != null) {
                keyPropMultiMap.put(type, set);
            }
            return this;
        }

        @Override
        public Cfg setKeyProps(Class<?> entityType, String... props) {
            ImmutableType type = ImmutableType.get(entityType);
            return setKeyProps(
                    Arrays.stream(props)
                            .map(type::getProp)
                            .toArray(ImmutableProp[]::new)
            );
        }

        @Override
        public <T extends Table<?>> Cfg setKeyProps(
                Class<T> tableType,
                Consumer<KeyPropCfg<T>> block
        ) {
            KeyPropCfgImpl<T> keyPropCfg = new KeyPropCfgImpl<T>(sqlClient, tableType);
            block.accept(keyPropCfg);
            return setKeyProps(
                    keyPropCfg.getProps().toArray(new ImmutableProp[0])
            );
        }

        @Override
        public Cfg setAutoAttaching(ImmutableProp prop) {
            validate();
            if (!prop.isEntityList()) {
                throw new IllegalArgumentException(
                        "Cannot set auto attaching for '" + prop + "' because it is not list property"
                );
            }
            autoAttachingSet.add(prop);
            return this;
        }

        @Override
        public Cfg setAutoAttaching(Class<?> entityType, String prop) {
            ImmutableType immutableType = ImmutableType.get(entityType);
            ImmutableProp immutableProp = immutableType.getProp(prop);
            return setAutoAttaching(immutableProp);
        }

        @Override
        public <T extends TableEx<?>> Cfg setAutoAttaching(
                Class<T> tableType,
                Function<T, Table<?>> block
        ) {
            ImmutableType immutableType = ImmutableType.get(tableType);
            TableImplementor<?> tableImpl = TableImplementor.create(
                    AbstractMutableStatementImpl.fake(sqlClient),
                    immutableType
            );
            T table = TableWrappers.wrap(tableImpl);
            TableImplementor<?> targetTableImpl = TableImplementor.unwrap(block.apply(table));
            if (targetTableImpl.getParent() != tableImpl) {
                throw new IllegalStateException("Lambda expression must return child table of specified table");
            }
            return setAutoAttaching(targetTableImpl.getJoinProp());
        }

        @Override
        public Cfg setAutoDetaching(ImmutableProp prop) {
            validate();
            if (!prop.isEntityList() || prop.getAssociationAnnotation().annotationType() != OneToMany.class) {
                throw new IllegalArgumentException(
                        "Cannot set auto detaching for '" + prop + "' because it is not one-to-many property"
                );
            }
            autoDetachingSet.add(prop);
            return this;
        }

        @Override
        public Cfg setAutoDetaching(Class<?> entityType, String prop) {
            ImmutableType immutableType = ImmutableType.get(entityType);
            ImmutableProp immutableProp = immutableType.getProp(prop);
            return setAutoDetaching(immutableProp);
        }

        @Override
        public <T extends TableEx<?>> Cfg setAutoDetaching(Class<T> tableType, Function<T, Table<?>> block) {
            ImmutableType immutableType = ImmutableType.get(tableType);
            TableImplementor<?> tableImpl = TableImplementor.create(
                    AbstractMutableStatementImpl.fake(sqlClient),
                    immutableType
            );
            T table = TableWrappers.wrap(tableImpl);
            TableImplementor<?> targetTableImpl = TableImplementor.unwrap(block.apply(table));
            if (targetTableImpl.getParent() != tableImpl) {
                throw new IllegalStateException("Lambda expression must return child table of specified table");
            }
            return setAutoDetaching(targetTableImpl.getJoinProp());
        }

        public Data freeze() {
            if (!frozen) {
                keyPropMultiMap = Collections.unmodifiableMap(keyPropMultiMap);
                autoAttachingSet = Collections.unmodifiableSet(autoAttachingSet);
                autoDetachingSet = Collections.unmodifiableSet(autoDetachingSet);
                frozen = true;
            }
            return this;
        }

        private void validate() {
            if (frozen) {
                throw new IllegalStateException("The current configuration is frozen");
            }
        }
    }

    private static class KeyPropCfgImpl<T extends Table<?>> implements KeyPropCfg<T> {

        private T table;

        private TableImplementor<?> tableImpl;

        private List<ImmutableProp> props = new ArrayList<>();

        @SuppressWarnings("unchecked")
        KeyPropCfgImpl(SqlClient sqlClient, Class<T> tableType) {
            this.tableImpl = TableImplementor.create(
                    AbstractMutableStatementImpl.fake(sqlClient),
                    ImmutableType.get(tableType)
            );
            this.table = (T)TableWrappers.wrap(tableImpl);
        }

        public List<ImmutableProp> getProps() {
            return props;
        }

        @Override
        public KeyPropCfg<T> addKeyProp(Function<T, PropExpression<?>> block) {
            PropExpressionImpl<?> expr = (PropExpressionImpl<?>) block.apply(table);
            if (expr.getTable() != tableImpl) {
                throw new IllegalStateException("Lambda expression must return expression on specified table");
            }
            props.add(expr.getProp());
            return this;
        }
    }
}
