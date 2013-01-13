package integration.tests;

import static fr.doan.achilles.columnFamily.ColumnFamilyHelper.normalizerAndValidateColumnFamilyName;
import static fr.doan.achilles.common.CassandraDaoTest.getCluster;
import static fr.doan.achilles.common.CassandraDaoTest.getEntityDao;
import static fr.doan.achilles.common.CassandraDaoTest.getKeyspace;
import static fr.doan.achilles.entity.metadata.PropertyType.JOIN_SIMPLE;
import static org.fest.assertions.api.Assertions.assertThat;
import integration.tests.entity.Tweet;
import integration.tests.entity.TweetTestBuilder;
import integration.tests.entity.User;
import integration.tests.entity.UserTestBuilder;

import java.util.List;
import java.util.UUID;

import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;

import org.apache.cassandra.utils.Pair;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import fr.doan.achilles.dao.GenericDynamicCompositeDao;
import fr.doan.achilles.entity.factory.ThriftEntityManagerFactoryImpl;
import fr.doan.achilles.entity.manager.ThriftEntityManager;
import fr.doan.achilles.exception.AchillesException;
import fr.doan.achilles.serializer.SerializerUtils;

/**
 * JoinColumnIT
 * 
 * @author DuyHai DOAN
 * 
 */
public class JoinColumnIT
{

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private final String ENTITY_PACKAGE = "integration.tests.entity";

	private GenericDynamicCompositeDao<UUID> tweetDao = getEntityDao(SerializerUtils.UUID_SRZ,
			normalizerAndValidateColumnFamilyName(Tweet.class.getCanonicalName()));

	private ThriftEntityManagerFactoryImpl factory = new ThriftEntityManagerFactoryImpl(
			getCluster(), getKeyspace(), ENTITY_PACKAGE, true);

	private ThriftEntityManager em = (ThriftEntityManager) factory.createEntityManager();

	private Tweet tweet;
	private User creator;
	private Long creatorId = RandomUtils.nextLong();

	@Before
	public void setUp()
	{
		creator = UserTestBuilder.user().id(creatorId).firstname("fn").lastname("ln").buid();
	}

	@Test
	public void should_persist_user_and_then_tweet() throws Exception
	{

		em.persist(creator);

		tweet = TweetTestBuilder.tweet().randomId().content("this is a tweet").creator(creator)
				.buid();

		em.persist(tweet);

		DynamicComposite startComp = new DynamicComposite();
		startComp.addComponent(0, JOIN_SIMPLE.flag(), ComponentEquality.EQUAL);

		DynamicComposite endComp = new DynamicComposite();
		endComp.addComponent(0, JOIN_SIMPLE.flag(), ComponentEquality.GREATER_THAN_EQUAL);

		List<Pair<DynamicComposite, Object>> columns = tweetDao.findColumnsRange(tweet.getId(),
				startComp, endComp, false, 20);

		assertThat(columns).hasSize(1);

		Pair<DynamicComposite, Object> creator = columns.get(0);
		assertThat(creator.right).isEqualTo(creatorId);

	}

	@Test
	public void should_find_user_from_tweet_after_persist() throws Exception
	{

		em.persist(creator);

		tweet = TweetTestBuilder.tweet().randomId().content("this is a tweet").creator(creator)
				.buid();

		em.persist(tweet);

		tweet = em.find(Tweet.class, tweet.getId());

		User joinUser = tweet.getCreator();

		assertThat(joinUser).isNotNull();
		assertThat(joinUser.getId()).isEqualTo(creatorId);
		assertThat(joinUser.getFirstname()).isEqualTo("fn");
		assertThat(joinUser.getLastname()).isEqualTo("ln");

	}

	@Test
	public void should_find_user_unchanged_from_tweet_after_merge() throws Exception
	{

		em.persist(creator);

		tweet = TweetTestBuilder.tweet().randomId().content("this is a tweet").creator(creator)
				.buid();

		creator.setFirstname("dfvdfv");
		creator.setLastname("fgbkl");

		tweet = em.merge(tweet);

		User joinUser = tweet.getCreator();

		assertThat(joinUser).isNotNull();
		assertThat(joinUser.getId()).isEqualTo(creatorId);
		assertThat(joinUser.getFirstname()).isEqualTo("fn");
		assertThat(joinUser.getLastname()).isEqualTo("ln");

	}

	@Test
	public void should_find_user_modified_from_tweet_after_refresh() throws Exception
	{

		em.persist(creator);

		tweet = TweetTestBuilder.tweet().randomId().content("this is a tweet").creator(creator)
				.buid();

		tweet = em.merge(tweet);

		User joinUser = tweet.getCreator();

		joinUser.setFirstname("changed_fn");
		joinUser.setLastname("changed_ln");

		em.merge(joinUser);

		em.refresh(tweet);

		joinUser = tweet.getCreator();

		assertThat(joinUser).isNotNull();
		assertThat(joinUser.getId()).isEqualTo(creatorId);
		assertThat(joinUser.getFirstname()).isEqualTo("changed_fn");
		assertThat(joinUser.getLastname()).isEqualTo("changed_ln");

	}

	@Test
	public void should_exception_when_persisting_join_user_without_existing_entity_in_db()
			throws Exception
	{

		creator = UserTestBuilder.user().id(RandomUtils.nextLong()).buid();
		tweet = TweetTestBuilder.tweet().randomId().content("this is a tweet").creator(creator)
				.buid();

		expectedEx.expect(AchillesException.class);
		expectedEx
				.expectMessage("The entity '"
						+ User.class.getCanonicalName()
						+ "' with id '"
						+ creator.getId()
						+ "' cannot be found. Maybe you should persist it first or set enable CascadeType.PERSIST");
		em.persist(tweet);
	}

	@After
	public void tearDown()
	{
		tweetDao.truncate();
	}
}