-- MySQL dump 10.13  Distrib 5.6.10, for Linux (x86_64)
--
-- Host: localhost    Database: hg19
-- ------------------------------------------------------
-- Server version	5.6.10

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `snp138`
--

DROP TABLE IF EXISTS `snp138`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snp138` (
  `bin` smallint(5) unsigned NOT NULL,
  `chrom` varchar(31) NOT NULL,
  `chromStart` int(10) unsigned NOT NULL,
  `chromEnd` int(10) unsigned NOT NULL,
  `name` varchar(15) NOT NULL,
  `score` smallint(5) unsigned NOT NULL,
  `strand` enum('+','-') NOT NULL,
  `refNCBI` blob NOT NULL,
  `refUCSC` blob NOT NULL,
  `observed` varchar(255) NOT NULL,
  `molType` enum('unknown','genomic','cDNA') NOT NULL,
  `class` enum('unknown','single','in-del','het','microsatellite','named','mnp','insertion','deletion') NOT NULL,
  `valid` set('unknown','by-cluster','by-frequency','by-submitter','by-2hit-2allele','by-hapmap','by-1000genomes') NOT NULL,
  `avHet` float NOT NULL,
  `avHetSE` float NOT NULL,
  `func` set('unknown','coding-synon','intron','near-gene-3','near-gene-5','ncRNA','nonsense','missense','stop-loss','frameshift','cds-indel','untranslated-3','untranslated-5','splice-3','splice-5') NOT NULL,
  `locType` enum('range','exact','between','rangeInsertion','rangeSubstitution','rangeDeletion','fuzzy') NOT NULL,
  `weight` int(10) unsigned NOT NULL,
  `exceptions` set('RefAlleleMismatch','RefAlleleRevComp','DuplicateObserved','MixedObserved','FlankMismatchGenomeLonger','FlankMismatchGenomeEqual','FlankMismatchGenomeShorter','NamedDeletionZeroSpan','NamedInsertionNonzeroSpan','SingleClassLongerSpan','SingleClassZeroSpan','SingleClassTriAllelic','SingleClassQuadAllelic','ObservedWrongFormat','ObservedTooLong','ObservedContainsIupac','ObservedMismatch','MultipleAlignments','NonIntegerChromCount','AlleleFreqSumNot1','SingleAlleleFreq','InconsistentAlleles') NOT NULL,
  `submitterCount` smallint(5) unsigned NOT NULL,
  `submitters` longblob NOT NULL,
  `alleleFreqCount` smallint(5) unsigned NOT NULL,
  `alleles` longblob NOT NULL,
  `alleleNs` longblob NOT NULL,
  `alleleFreqs` longblob NOT NULL,
  `bitfields` set('clinically-assoc','maf-5-some-pop','maf-5-all-pops','has-omim-omia','microattr-tpa','submitted-by-lsdb','genotype-conflict','rs-cluster-nonoverlapping-alleles','observed-mismatch') NOT NULL,
  KEY `name` (`name`),
  KEY `chrom` (`chrom`,`bin`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-04-27 14:15:03
